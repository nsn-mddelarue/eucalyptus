#!/bin/sh


# Login, and get session id
wget -O /tmp/sessionId --no-check-certificate 'https://localhost:8443/loginservlet?adminPw=admin'
export SESSIONID=`cat /tmp/sessionId`
echo "session id:" $SESSIONID


if [ -n $EUCALYPTUS ]; then
        export EUCALYPTUS="/opt/eucalyptus"
fi

# Get mysql password
password=`./dbPass.sh`


# Clear all prior data 
wget --no-check-certificate -O /tmp/nothing "https://localhost:8443/commandservlet?sessionId=$SESSIONID&className=com.eucalyptus.reporting.s3.FalseDataGenerator&methodName=removeAllData"

if [ "$?" -ne "0" ]
then
	echo "Wget failed to clear all prior data."
	exit -1
fi

# Check that the data is cleared
LINE_CNT=`mysql -u eucalyptus --password=$password -P 8777 --protocol=TCP --database=eucalyptus_reporting --execute="select count(*) from s3_usage_snapshot"|awk '/[0-9]+/ {print $1}'`
if [ "$LINE_CNT" -ne "0" ]
then
	echo "Data not cleared"
	exit -1
else
	echo "Data cleared"
fi


# Generate data
timestamp=`date +%s`
timestamp=$(($timestamp*1000))
./s3curl.pl --id $EC2_ACCESS_KEY --key $EC2_SECRET_KEY --put /dev/null -- -s -v $S3_URL/mybucket
./s3curl.pl --id $EC2_ACCESS_KEY --key $EC2_SECRET_KEY --put data.txt -- -s -v $S3_URL/mybucket/obj_${timestamp}_a
./s3curl.pl --id $EC2_ACCESS_KEY --key $EC2_SECRET_KEY --put data.txt -- -s -v $S3_URL/mybucket/obj_${timestamp}_b
./s3curl.pl --id $EC2_ACCESS_KEY --key $EC2_SECRET_KEY --put data.txt -- -s -v $S3_URL/mybucket/obj_${timestamp}_c


# Check that the data exists and has been inserted 
LINE_CNT=`mysql -u eucalyptus --password=$password -P 8777 --protocol=TCP --database=eucalyptus_reporting --execute="select count(*) from s3_usage_snapshot"|awk '/[0-9]+/ {print $1}'`
if [ "$LINE_CNT" -ne "0" ]
then
	echo "Data generated"
else
	echo "Data not generated"
	exit -1
fi


# Check that the inserted data has correct timestamps
error_margin=$((60*60*1000)) # 1 hr

mysql -u eucalyptus --password=$password -P 8777 --protocol=TCP --database=eucalyptus_reporting --execute="select timestamp_ms from s3_usage_snapshot"|awk '/[0-9]+/ {print $1}'| while read line; do
	is_within=`echo "within($line,$timestamp,$error_margin)"|bc within_error.bc`
	if [ "$is_within" -eq "1" ]
	then
		echo "line:$line correct:$timestamp within:true"
	else
		echo "line:$line correct:$timestamp within:false"
		exit -1
	fi
done

exit 0

