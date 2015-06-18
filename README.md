# VMAvailabilityManager
Availability manager for monitoring the VM and Disaster recovery system

# VMAvailabilityManager
--------------

> Individual project developed for CMPE283 (Spring 2015)
> at San Jose State University
 - Title: Disaster Recoery Manager
 
##Features supported
--------------
 - Suport for backup cache which updates every 10 minutes
 - Periodic monitoring of the virtual machines for any failures
 - Recovery system for the failed virtual machines and VHosts.
 - Support of alarm notifications on VHosts manual power off.
 - Understanding of VMWare VI Java APIs
 
##Basic Configuration
--------------
* VMWare ESXi installed on team data center, atleast with 3 VHosts.
* VM running on Ubuntu 32bit and VMWare tools installed.
* VSphere Management client on windows.

##Tools being used
--------------
* VMWare tools installed on each VM.
* VSphere Management client
* VMWare Infrastructure(VI) Apis

##Project Architecture diagram
----------------------------------------
![Design](/diagram/ClassDiagram.png?raw=true)