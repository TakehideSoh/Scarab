VER = v1-8-9
VERSION = 1.8.9
VER0 = 189

APP0 = scarab
APP = $(APP0)$(VER0)
JAR = $(APP).jar
ZIP = $(APP).zip

WEBPAGE = http://kix.istc.kobe-u.ac.jp/~soh/scarab/
WEBTITLE = Scarab: A Rapid Prototyping Tool for SAT-based Systems
SRCS = src/main/scala/jp/kobe_u/$(APP0)/*.scala 

DOCTITLE = Scarab version $(VERSION) Core API Specification
SCALADOC  = scaladoc \
	-d docs/api \
	-doc-title '$(DOCTITLE)' \
	-doc-version '$(VERSION)' \
	-classpath classes \
	-sourcepath src

all: scalac jar zip

scalac:
	rm -rf bin/jp
	mkdir -p bin/jp
	fsc -reset
	fsc -deprecation -sourcepath src -d bin -cp bin -optimise $(SRCS)

jar:
	jar cf $(JAR) -C bin .

scaladoc:
	rm -rf docs/api/*
	mkdir -p docs/api
	$(SCALADOC) $(SRCS)

zip:
	rm -f ../$(ZIP)
	rm -rf $(APP)
	mkdir ../$(APP)
	find ./* -name '.save.log' -exec rm '{}' \;	
	cp -pr * ../$(APP)
	find ../$(APP) \( -name .svn -o -name CVS -o -name .cvsignore -o -name '*~' \) -exec rm -r '{}' '+'
	zip -q -r ../$(ZIP) ../$(APP)
	rm -rf ../$(APP)

web:
	cp ../$(ZIP) ~/06_web/www/scarab/
	cp $(JAR) ~/06_web/www/scarab/
	rm -R ~/06_web/www/scarab/docs
	cp -r docs ~/06_web/www/scarab/

clean:
	rm -rf bin/*
	rm -rf docs/api/*

