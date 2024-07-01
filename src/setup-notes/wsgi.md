# WSGI server setup

Run a WSGI server and expose your Flask app with a URL endpoint.

### Step 1: Install WSGI server

First, we need to install our WSGI server on our Debian machine:

```bash
pip3 install gunicorn
```
You will need to install your WSGI server in our Python venv too but that is later.

### Step 2: Create a systemd service
Next, we create a systemd service so our Gunicorn server starts on boot.

```bash
vim /etc/systemd/system/loquax.service

[Unit]
Description=Gunicorn instance to serve Loquax
After=network.target

[Service]
User=root
Group=root

 # Sets the working directory for the Gunicorn process.
WorkingDirectory=/var/www/loquax

 # Sets the PATH environment variable so it includes the directory
 # where the Python virtual environment is located.
Environment="PATH=/var/www/loquax/venv/bin"

 # The command to start the Gunicorn server with 3 worker processes,
 # binding to all network interfaces on port 8000
ExecStart=/var/www/loquax/venv/bin/gunicorn --workers 3 \
--bind 0.0.0.0:8000 app:app

[Install]
WantedBy=multi-user.target
```

### Step 3: Configure Nginx
Now you want nginx or your web server to bind to the same port as your WSGI server w/ URL prefix. 
Add the below "loquax" block to your nginx "server" block:

```bash
sudo vim /etc/nginx/sites-available/mywebsite
```
```nginx
server {
    server_name nargothrond.xyz;
    root /var/www/mysite;
    index index.html index.htm index.nginx-debian.html;
    client_max_body_size 64M;

    location / {
        try_files $uri $uri/ =404;
    }

    proxy_set_header X-Forwarded-For $remote_addr;

    location /loquax/ {
        include proxy_params;
        proxy_pass http://localhost:8000/;
    }
}
```

### Step 4: Start up your WSGI server

```bash
systemctl daemon-reload
systemctl enable loquax
systemctl start loquax
```

### Step 5: Run your app
Ensure your app.py, index.html, and all the things your Flask app will link in are in 
the same path specified in your "Step 2" systemd service, i.e:

```bash
ls -l /var/www/loquax

-rw-r--r-- 1 root root  764 Jul  5 07:09 app.py
drwxr-xr-x 2 root root 4096 Jul  5 07:09 __pycache__
-rw-r--r-- 1 root root   21 Jul  5 07:09 requirements.txt
drwxr-xr-x 2 root root 4096 Jun 27 21:00 static
drwxr-xr-x 2 root root 4096 Jun 27 21:00 templates
drwxr-xr-x 5 root root 4096 Jul  5 04:43 venv
```
Don't forget to reactivate your virtual env if you update some packages your Flask app relies on.

```bash
 # Define the remote path to the project
REMOTE_PATH="/var/www/loquax"
VENV_PATH="${REMOTE_PATH}/venv"

 # Check if the venv exists, if not create it
if [ ! -d "${VENV_PATH}" ]; then
    python3.10 -m venv ${VENV_PATH} \
    || echo 'Failed to create virtual environment';
else
    echo 'Virtual environment already exists';
fi;

 # Activate the virtual environment
source ${VENV_PATH}/bin/activate;
```
