# Author: Vishnu Raveendra Nadhan
# Ver.2
#!/bin/bash

if [ "$1" == "" ]
then
	echo "Invalid arguments! Run file as ./startServer.sh <file-name>"
	exit 0
fi

declare ips 

# Here, || [[ -n $line ]] prevents the last line from being ignored if it doesn't end with a \n (since read returns a non-zero exit code when it encounters EOF).
while read -r line ||  [[ -n "$line" ]]; do
	if [ "$line" ]
	then
		ips+=${line// /;}
		ips+=" "
	fi
done < "$1"

# echo "$ips"

# First, compile the Java programs into respective class files
javac -cp ".:jar/*" src/RemoteCameraServer.java
  
# Now pass the arguments to the Java classes
java -cp ".:src/:jar/*" RemoteCameraServer $ips