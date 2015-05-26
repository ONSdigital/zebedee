from java

WORKDIR /usr/src/zebedee
RUN git clone https://github.com/Carboni/zebedee.git

CMD ["files.sh"]
