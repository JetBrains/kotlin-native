#!/usr/bin/env bash
systemctl enable graphical.target --force
systemctl set-default graphical.target

systemctl enable cups.service
systemctl start cups.service
systemctl enable cups.socket
systemctl start cups.socket
systemctl enable cups.path
systemctl start cups.path
 
systemctl enable cups-browsed
systemctl start cups-browsed
 
systemctl start avahi-daemon.socket
systemctl start avahi-daemon.service
systemctl enable avahi-daemon.service
systemctl enable avahi-daemon.socket
 
systemctl start whoopsie.service
systemctl enable whoopsie.service
systemctl start upower.service
systemctl enable upower.service
 
systemctl start snapd.socket
systemctl enable snapd.socket
systemctl start snapd.service
systemctl enable snapd.service
 
systemctl start bluetooth.target
systemctl enable bluetooth.target
systemctl start bluetooth.service
systemctl enable bluetooth.service