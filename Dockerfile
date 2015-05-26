from java

WORKDIR /usr/src
RUN git clone https://github.com/Carboni/zebedee.git
WORKDIR zebedee

CMD ["./files.sh"]
