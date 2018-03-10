import pygit2
import lxml
import requests
from lxml import etree
import datetime

# https://stackoverflow.com/questions/2150739/iso-time-iso-8601-in-python
# http://lxml.de/xpathxslt.html
# https://stackoverflow.com/questions/5234090/how-to-take-the-first-n-items-from-a-generator-or-list-in-python
# http://lxml.de/1.3/tutorial.html#elements-carry-attributes
# http://docs.python-requests.org/en/master/user/quickstart/#make-a-request
repo = pygit2.Repository('..')
new_date = datetime.datetime.now().isoformat()
for websub_file in repo.config.get_multivar("websub.files"):
    with open(websub_file, 'r') as f:
        html = etree.parse(f)
    for self_url_link in html.xpath('//link[@rel="self"]')[:1]:
        self_url = self_url_link.get("href")
        for hub_url_link in html.xpath('//link[@rel="hub"]')[:1]:
            hub_url = hub_url_link.get("href")
            for updated_meta in html.xpath('//meta[@name="updated"]')[:1]:
                updated_meta.set("content", new_date)
                with open(websub_file, 'w') as f:
                    f.write(etree.tostring(html, encoding='unicode'))
                requests.post(hub_url, data={'hub.mode': 'publish', 'hub.url': self_url})
