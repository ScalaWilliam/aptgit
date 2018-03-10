import os
import pygit2
import sys
from lxml import etree
import datetime

GIT_DIR = os.getenv('GIT_DIR', os.getcwd())
repo = pygit2.Repository(GIT_DIR)
new_self_url_link = sys.argv[-1]
new_date = datetime.datetime.now().isoformat()
for websub_file in repo.config.get_multivar("websub.files"):
    with open(websub_file, 'r') as f:
        html = etree.parse(f)
    for self_url_link in html.xpath('//link[@rel="self"]')[:1]:
        self_url_link.set("href", new_self_url_link)
        output_html = etree.tostring(html, encoding='unicode')
        with open(websub_file, 'w') as f:
            f.write(output_html)
