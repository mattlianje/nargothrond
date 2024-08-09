# Calibre Server Setup
Setting up a Calibre book server for your epubs (with a URl prefix).

### Step 1: Install Calibre
First we need to install calibre on our server

```bash
apt install -y calibre
mkdir /opt/calibre
```

### Step 2: Create the Calibre library

Next, we create the library where our books will be stored:

```bash
rsync -avuP YOUR_LIBRARY_DIR root@nargothrond.xyz:/opt/calibre/
calibredb add /opt/calibre/your-local-dir/*.epub \
    --with-library nargothrond-library
```

This is how you add a user to protect your books:

```bash
calibre-server --manage-users
```

### Step 3: Create a systemd service

So the Calibre server starts automatically on boot, we create a systemd
service (the option "--url-prefix /calibre" is imperative if you want to
use a url prefix ... [Calibre doc](https://manual.calibre-ebook.com/server.html)

```bash
vim /etc/systemd/system/calibre-server.service

[Unit]
Description=Calibre library server
After=network.target

[Service]
Type=simple
User=root
Group=root
ExecStart=/usr/bin/calibre-server \
--enable-auth --enable-local-write /opt/calibre/nargothrond-library \
--listen-on 127.0.0.1 --url-prefix /calibre --port 8080

[Install]
WantedBy=multi-user.target
```

Now you can start your calibre service as show below:
```bash
systemctl daemon-reload
systemctl enable calibre-server
systemctl start calibre-server
```

### Step 4: Configure Nginx
Add a block w/ url prefix to your existing Nginx config:
```bash
vim /etc/nginx/sites-available/mywebsite

proxy_set_header X-Forwarded-For $remote_addr;
location /calibre/ {
    proxy_buffering off;
    proxy_pass http://127.0.0.1:8080$request_uri;
 }
location /calibre {
    # we need a trailing slash for the Application Cache to work
    rewrite /calibre /calibre/ permanent;
 }
```

