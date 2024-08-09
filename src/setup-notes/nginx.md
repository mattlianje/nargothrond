# Server and Domain Setup

Notes for setting up a webserver away from prying eyes. We will be using nginx as our web server and also setting up an SSL certificate with Let's Encrypt for secure connections.

### Step 1: Setting up DNS records

First, we need to point our domain name to the IP address of our server. 
This is done by adding A and AAAA records with our DNS provider. 
Replace the 'ipv4' and 'ipv6' placeholders with your server's actual IP addresses:

```plaintext
A record: nargothrond.xyz -> ipv4
AAAA record: nargothrond.xyz -> ipv6
```

### Step 2: SSH into your server
Connect to your server via SSH. Here, we are using the domain name 'nargothrond.xyz'.
```bash
ssh root@nargothrond.xyz
```

### Step 3: Install Nginx
Update your server's package list and install Nginx:

```bash
sudo apt update
sudo apt upgrade
sudo apt install nginx
```

### Step 4: Configure Nginx
Create a new Nginx configuration file using vim and set up your server block. 
Replace 'nargothrond.xyz' with your actual domain name and '/var/www/mysite' with the path to your website's files:

```bash
sudo vim /etc/nginx/sites-available/mywebsite
```
```nginx
server {
    listen 80;
    listen [::]:80;

    server_name nargothrond.xyz;
    root /var/www/mysite;
    index index.html index.htm index.nginx-debian.html;

    location / {
        try_files $uri $uri/ =404;
    }
}
```

### Step 5: Enable your Nginx configuration
Create a symbolic link from your new configuration file to the sites-enabled directory:

```bash
sudo ln -s /etc/nginx/sites-available/mywebsite /etc/nginx/sites-enabled
```

### Step 6: Install Certbot
Install Certbot and the Nginx plugin:

```bash
sudo apt install python3-certbot-nginx
```

### Step 7: Obtain SSL certificate
Run Certbot to obtain and install your SSL certificate:
```bash
sudo certbot --nginx
```

### Step 8: Set up automatic certificate renewal
Edit your crontab file to add a new cron job that will run the 'certbot renew' command every month:
```bash
crontab -e

0 0 1 * * /usr/bin/certbot renew --quiet
```

