# Jitsi setup

Self-host Jitsi video call server to talk to your contacts securely.

### Step 1: Update DNS tables

Ensure you create A and AAAA records for meet.nargothrond.xyz subdomains.

Note: There are port conflicts if you try to run vanilla Jitsi alongside an existing prosody or ejabberd XMPP server since Jitsi uses XMPP for presence.

### Step 2: Install Jitsi

Next, we install Jitsi. The Jitsi documentation is very good for Debian servers and the [Jitsi official self-hosting instructions](https://jitsi.github.io/handbook/docs/devops-guide/devops-guide-quickstart) worked out of the box.

### Step 3: Securing Jitsi

Finally, follow these instructions to secure our Jitsi server: [Jitsi secure instructions](https://jitsi.github.io/handbook/docs/devops-guide/secure-domain/).

