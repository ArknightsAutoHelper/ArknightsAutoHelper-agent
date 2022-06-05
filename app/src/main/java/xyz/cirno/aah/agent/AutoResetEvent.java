package xyz.cirno.aah.agent;

public class AutoResetEvent
{
    private final Object _monitor = new Object();
    private volatile boolean _isOpen = false;

    public AutoResetEvent(boolean open)
    {
        _isOpen = open;
    }

    public void waitOne() throws InterruptedException
    {
        synchronized (_monitor) {
            while (!_isOpen) {
                _monitor.wait();
            }
            _isOpen = false;
        }
    }

    public void waitOne(long timeout) throws InterruptedException
    {
        long nanotimeout = timeout * 1000000;
        synchronized (_monitor) {
            long t = System.nanoTime();
            while (!_isOpen) {
                _monitor.wait(timeout);
                // Check for timeout
                if (System.nanoTime() - t >= nanotimeout)
                    break;
            }
            _isOpen = false;
        }
    }

    public void set()
    {
        synchronized (_monitor) {
            _isOpen = true;
            _monitor.notify();
        }
    }

    public void reset()
    {
        _isOpen = false;
    }
}
