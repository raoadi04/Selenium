using System;

namespace OpenQA.Selenium.Internal.Logging
{
    public interface ILogHandler
    {
        void Handle(LogEvent logEvent);

        ILogHandler Clone();
    }
}
