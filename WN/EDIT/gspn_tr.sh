#!/bin/sh 
############ GreatSPN2.0 ######################
#if ( ! $?GSPN2VERSION ) then
#	set path1 = $0;
#	set path2 = $path1:h
#	set path1 = $path2:h
#        setenv GSPN2VERSION $path1/install/version.csh
#	unset path1
#	unset path2
#endif
#if ( ! $?GSPN2BINS ) then
#        setenv GSPN2BINS `$GSPN2VERSION`
#endif
#if ( ! $?GSPN2HOME ) then
#        source ~/.GreatSPN2_$GSPN2BINS
#endif
. great_package.sh

echo ""
echo "Start EGSPN Transient construction"
#time
${GREATSPN_BINDIR}/GSPNRG $1 -m
if [ $? -ne 0 ] 
then
	exit 37
fi
/bin/cat /dev/null >  $1.gst
if [ $? -ne 0 ] 
then
	exit 32 
fi
${GREATSPN_BINDIR}/swn_stndrd $1
if [ $? -ne 0 ] 
then
   exit 24
fi
${GREATSPN_BINDIR}/swn_ntrs $1 $2 -e0.000001 -i10000
if [ $? -ne 0 ] 
then
	exit 36
fi
/bin/cp $1.epd $1.mpd
if [ $? -ne 0 ] 
then
	exit 31 
fi
${GREATSPN_BINDIR}/swn_gst_prep $1
if [ $? -ne 0 ] 
then
	exit 27 
fi
${GREATSPN_BINDIR}/swn_gst_stndrd $1 > $1.trash
if [ $? -ne 0 ] 
then
	exit 26 
fi
/bin/cat $1.trash $1.sta > $1.temp
if [ $? -ne 0 ] 
then
	exit 32 
fi
/bin/mv $1.temp $1.sta
if [ $? -ne 0 ] 
then
	exit 33 
fi
/bin/rm $1.trash
if [ $? -ne 0 ] 
then
	exit 30 
fi
exit 0
