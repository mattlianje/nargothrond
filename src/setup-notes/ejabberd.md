# Ejabberd XMPP server setup

Self-host an ejabberd XMPP server, a responsible and secure way of communicating w/ other JID's on a server that doesn't spy on you.

### Step 1: Update DNS tables

Ensure you have created A and AAAA records for all of the below subdomains with your registrar. A *.nargothrond.xyz will not suffice.
```plaintext
nargothrond.xyz - Your XMPP hostname 
                ... Ask yourself what you want after the @ in your JID's 
                ... of the form bombadil@nargothrond.xyz
conference.nargothrond.xyz - For Multi User Chats (MUCs)
upload.nargothrond.xyz - For mod_http_upload, file upload support
proxy.nargothrond.xyz - For mod_proxy65, SOCKS5 proxy support
pubsub.nargothrond.xyz- For mod_pubsub, publish-subscribe support 
                        (A fancier RSS)
```

### Step 2: Install Ejabberd
Next, install ejabberd, first by adding the ejabberd package repo:
```bash
curl -o /etc/apt/sources.list.d/ejabberd.list \ 
    https://repo.process-one.net/ejabberd.list
curl -o /etc/apt/trusted.gpg.d/ejabberd.gpg \
    https://repo.process-one.net/ejabberd.gpg
```

Then install the ejabberd package:
```bash
apt update
apt install ejabberd
```

### Step 3: Create/move certificates
Ejabberd doesn't have a script to create and move certificates to a place where its server can read them so we do this manually. TODO: add this script to my crontab for auto-renewal of certificates.
```bash
DOMAIN=nargothrond.xyz

declare -a subdomains=("" "conference." "proxy." "pubsub." "upload.")

for i in "${subdomains[@]}"; do
    certbot --nginx -d $i$DOMAIN certonly
    mkdir -p /etc/ejabberd/certs/$i$DOMAIN
    cp /etc/letsencrypt/live/$i$DOMAIN/fullchain.pem \
        /etc/ejabberd/certs/$i$DOMAIN
    cp /etc/letsencrypt/live/$i$DOMAIN/privkey.pem \ 
        /etc/ejabberd/certs/$i$DOMAIN
done
```

Next, the ejabberd user must be able to read these certs:
```bash
chown -R ejabberd:ejabberd /etc/ejabberd/certs
```

And finally we update the ejabberd conf. The location of the conf seems to vary between ejabberd version but "/opt/ejabberd/conf/ejabberd.yml" seems to be the most recent.
```bash
vim /opt/ejabberd/conf/ejabberd.yml

certfiles:
  - "/etc/ejabberd/certs/*/*.pem"
```

### Step 4: Admin user, archives and fw
We will use the default Mnesia database with a 2GB limit.

Set your admin in the conf like below:

```bash
vim /opt/ejabberd/conf/ejabberd.yml

acl:
  admin:
    user: matthieu
```

And optionally enable archiving by uncommenting:
```bash
vim /opt/ejabberd/conf/ejabberd.yml

mod_mam:
  assume_mam_usage: true
  default: always
```

Next, register the admin from above via ejabberdctl:
```bash
su -c "ejabberdctl register matthieu nargothrond.xyz password" ejabberd
```

Finally, restart ejabberd and make sure port 5280 is open (trickster):
```bash
ufw allow 5280
systemctl restart ejabberd
```
