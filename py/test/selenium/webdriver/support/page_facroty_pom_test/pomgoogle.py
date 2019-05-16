from selenium.webdriver.support.page_facroty import cacheable, callable_find_by as find_by
from selenium.webdriver.common.by import By

class GooglePage(object):

    _search_box = find_by(how=By.NAME, using="q", cacheable=True)

    def __init__(self, driver):
        cacheable(lookup=self)
        self._driver = driver
        self.url = self._driver.current_url


    def search_word_submit(self, word):
        self._search_box().clear()
        self._search_box().send_keys(word)
        self._search_box().submit()
