FROM node:23-bookworm

# RUN apt update; \
#     apt install -y --no-install-recommends 

# Setting up the work directory in image
WORKDIR /express

# copy from local machine to the image.
COPY ./express/index.js /express/index.js

RUN npm install express
RUN npm install prom-client

# Exposing server port
EXPOSE 3000

# Starting our application
CMD [ "node", "index.js" ]
