ifdef TOP
DEV_ROOT = ${TOP}/.
else
DEV_ROOT = .
endif

# This is where the OT classes are found
OTROOT = /jac_sw/hlsroot/acsis_support/acsisot
OTROOT = /jac_sw/orac3

JAVAPACKAGES = \
	edu/jach/qt/utils \
	edu/jach/qt/gui \
	edu/jach/qt/djava \
	edu/jach/qt/app

JAVAMAIN = edu.jach.qt/app/QT

JAVALIB = qt.jar
JAVASRCLIB = qt-src.jar

JAVALIBRARIES = \
	activation.jar \
	diva.jar \
	jfcunit.jar \
	jsky.jar \
	junit.jar \
	soap.jar \
	mail.jar \
	optional.jar \
	xercesImpl.jar \
	xmlParserAPIs.jar \
	calpahtml.jar

EXTERNALCLASSES = \
	/local/java/jdk/jre/lib/ext/log4j-1.2rc1.jar \
	$(OTROOT)/GEMINI/install/classes \
	$(OTROOT)/ORAC/install/classes \
	/jac_sw/drama/CurrentRelease/javalib \
	$(OTROOT)/OT/install/classes \
	$(OTROOT)/OMP/install/classes \
	/jac_sw/itsroot/install/dcHub/javalib/dcHub.jar \
	/star/starjava/lib/pal/pal.jar

ifdef SOURCES
include ../../../../../../make.tail
else
include make.tail
endif
