from java

RUN apt-get update && sudo apt-get -y dist-upgrade
RUN apt-get install -y git maven 

WORKDIR /usr/src
RUN git clone https://github.com/Carboni/zebedee.git
WORKDIR zebedee

CMD ["./files.sh"]
