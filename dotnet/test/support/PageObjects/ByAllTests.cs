﻿using System.Collections.Generic;
using NMock2;
using NUnit.Framework;
using Is = NUnit.Framework.Is;

namespace OpenQA.Selenium.Support.PageObjects
{
    [TestFixture]
    class ByAllTests
    {
        [Test]
        public void FindElementZeroBy()
        {
            var mock = new Mockery();
            var driver = mock.NewMock<IAllDriver>();

            var by = new ByAll();

            Assert.Throws<NoSuchElementException>(() => by.FindElement(driver));
            Assert.That(by.FindElements(driver), Is.EqualTo(new List<IWebElement>().AsReadOnly()));
        }

        [Test]
        public void FindElementOneBy()
        {
            var mock = new Mockery();
            var driver = mock.NewMock<IAllDriver>();
            var elem1 = mock.NewMock<IAllElement>();
            var elem2 = mock.NewMock<IAllElement>();
            var elems12 = new List<IWebElement> { elem1, elem2 }.AsReadOnly();
            Expect.Exactly(2).On(driver).Method("FindElementsByName").With("cheese").Will(Return.Value(elems12));
            var by = new ByAll(By.Name("cheese"));

            // findElement
            Assert.AreEqual(by.FindElement(driver), elem1);
            //findElements
            Assert.That(by.FindElements(driver), Is.EqualTo(elems12));

            mock.VerifyAllExpectationsHaveBeenMet();
        }
        
        [Test]
        public void FindElementOneByEmpty()
        {
            var mock = new Mockery();
            var driver = mock.NewMock<IAllDriver>();
            var empty = new List<IWebElement>().AsReadOnly();

            Expect.Exactly(2).On(driver).Method("FindElementsByName").With("cheese").Will(Return.Value(empty));

            var by = new ByAll(By.Name("cheese"));

            // one element
            Assert.Throws<NoSuchElementException>(() => by.FindElement(driver));
            Assert.That(by.FindElements(driver), Is.EqualTo(empty));

            mock.VerifyAllExpectationsHaveBeenMet();
        }
        
        [Test]
        public void FindElementTwoBy()
        {
            var mocks = new Mockery();
            var driver = mocks.NewMock<IAllDriver>();

            var elem1 = mocks.NewMock<IAllElement>();
            var elem2 = mocks.NewMock<IAllElement>();
            var elem3 = mocks.NewMock<IAllElement>();
            var elems12 = new List<IWebElement> { elem1, elem2 }.AsReadOnly();
            var elems23 = new List<IWebElement> { elem2, elem3 }.AsReadOnly();

            Expect.Exactly(2).On(driver).Method("FindElementsByName").With("cheese").Will(Return.Value(elems12));
            Expect.Exactly(2).On(driver).Method("FindElementsByName").With("photo").Will(Return.Value(elems23));

            var by = new ByAll(By.Name("cheese"), By.Name("photo"));

            // findElement
            Assert.That(by.FindElement(driver), Is.EqualTo(elem2));

            //findElements
            var result = by.FindElements(driver);
            Assert.That(result.Count, Is.EqualTo(1));
            Assert.That(result[0], Is.EqualTo(elem2));

            mocks.VerifyAllExpectationsHaveBeenMet();
        }

        [Test]
        public void FindElementDisjunct()
        {
            var mocks = new Mockery();
            var driver = mocks.NewMock<IAllDriver>();

            var elem1 = mocks.NewMock<IAllElement>();
            var elem2 = mocks.NewMock<IAllElement>();
            var elem3 = mocks.NewMock<IAllElement>();
            var elem4 = mocks.NewMock<IAllElement>();
            var elems12 = new List<IWebElement> { elem1, elem2 }.AsReadOnly();
            var elems34 = new List<IWebElement> { elem3, elem4 }.AsReadOnly();

            Expect.Exactly(2).On(driver).Method("FindElementsByName").With("cheese").Will(Return.Value(elems12));
            Expect.Exactly(2).On(driver).Method("FindElementsByName").With("photo").Will(Return.Value(elems34));

            var by = new ByAll(By.Name("cheese"), By.Name("photo"));
            
            Assert.Throws<NoSuchElementException>(() => by.FindElement(driver));

            var result = by.FindElements(driver);
            Assert.That(result.Count, Is.EqualTo(0));
            mocks.VerifyAllExpectationsHaveBeenMet();
        }

    }
}
