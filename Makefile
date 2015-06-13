VER = v1-6-9
VERSION = 1.6.9

APP0 = scarab
APP = $(APP0)-$(VER)
JAR = $(APP).jar
ZIP = $(APP).zip

WEBPAGE = http://kix.istc.kobe-u.ac.jp/~soh/scarab/
WEBTITLE = Scarab: A Rapid Prototyping Tool for SAT-based Systems
SRCS = src/jp/kobe_u/$(APP0)/*.scala src/jp/kobe_u/$(APP0)/*/*.scala

DOCTITLE = Scarab version $(VERSION) Core API Specification
SCALADOC  = scaladoc \
	-d docs/api \
	-doc-title '$(DOCTITLE)' \
	-doc-version '$(VERSION)' \
	-classpath classes \
	-sourcepath src

all: scalac jar scaladoc zip

scalac:
	rm -rf classes/*
	mkdir -p classes
	cp -Rp sat4j/org classes/
	fsc -reset
	fsc -deprecation -sourcepath src -d classes -cp classes -optimise $(SRCS)

jar:
	jar cf lib/$(JAR) -C classes .

scaladoc:
	rm -rf docs/api/*
	mkdir -p docs/api
	$(SCALADOC) $(SRCS)

zip:
	rm -f ../$(ZIP)
	rm -rf $(APP)
	mkdir $(APP)
	find ./* -name '.save.log' -exec rm '{}' \;	
	cp -pr Makefile src lib docs examples $(APP)
	rm -f $(APP)/lib/$(APP0)*.jar $(APP)/examples/classes/*
	cp -pr lib/$(JAR) $(APP)/lib
	find $(APP) \( -name .svn -o -name CVS -o -name .cvsignore -o -name '*~' \) -exec rm -r '{}' '+'
	zip -q -r ../$(ZIP) $(APP)
	rm -rf $(APP)

web:
	cp ../$(ZIP) ~/06_web/www/scarab/
	rm -R ~/06_web/www/scarab/docs
	cp -r docs ~/06_web/www/scarab/

clean:
	rm -rf classes/*
	rm -rf docs/api/*

