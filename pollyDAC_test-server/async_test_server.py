#!/usr/bin/python
import socket
import cherrypy
import os,os.path
import urlparse
import random
import time

class WebService():
    exposed = True
    @cherrypy.tools.json_out()
    def GET(self):
        for i in range(5, 0, -1):
            print i
            time.sleep(1)
        result = []
        for i in range(10):
            result.append(random.randint(0,999))
        return result

if __name__ == '__main__':
    server_config = {
        'server.socket_port': 8080,
        'server.socket_host': '0.0.0.0',
    }
    conf = {
        '/': {
            'request.dispatch' : cherrypy.dispatch.MethodDispatcher(),
        }
    }
    webService = WebService()
    cherrypy.config.update(server_config)
    cherrypy.quickstart(webService, '/', conf)
