from http.server import BaseHTTPRequestHandler, HTTPServer

"""
Inspiration: https://gist.github.com/mdonkers/63e115cc0c79b4f6b8b3a6b797e485c7
"""
import logging

class Server(BaseHTTPRequestHandler):
    def _set_headers(self):
        self.send_response(200)
        self.send_header('Content-type', 'text/html')
        self.end_headers()

    def do_POST(self):
        content_length = int(self.headers['Content-Length'])
        post_data = self.rfile.read(content_length)
        logging.info("POST %s" % str(self.path))
        logging.info("POST request,\nPath: %s\nHeaders:\n%s\n\nBody:\n%s\n",
                     str(self.path), str(self.headers), post_data.decode('utf-8'))
        self.wfile.write("POST request for {}".format(self.path).encode('utf-8'))

if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO)
    httpd = HTTPServer(('', 9001), Server)
    httpd.serve_forever()
