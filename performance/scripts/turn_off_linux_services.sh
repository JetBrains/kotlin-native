#!/usr/bin/env bash
systemctl enable multi-user.target --force
systemctl set-default multi-user.target

systemctl disable cups.service
systemctl stop cups.service
systemctl disable cups.socket
systemctl stop cups.socket
systemctl disable cups.path
systemctl stop cups.path
 
systemctl disable cups-browsed
systemctl stop cups-browsed
 
systemctl stop avahi-daemon.socket
systemctl stop avahi-daemon.service
systemctl disable avahi-daemon.service
systemctl disable avahi-daemon.socket
 
systemctl stop whoopsie.service
systemctl disable whoopsie.service
systemctl stop upower.service
systemctl disable upower.service
 
systemctl stop snapd.socket
systemctl disable snapd.socket
systemctl stop snapd.service
systemctl disable snapd.service
 
systemctl stop bluetooth.target
systemctl disable bluetooth.target
systemctl stop bluetooth.service
systemctl disable bluetooth.service