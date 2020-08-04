package chat.dim.ui.grid;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.widget.GridView;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import chat.dim.ui.list.DummyList;

public class GridFragment<VA extends GridViewAdapter, L extends DummyList> extends Fragment {

    protected L dummyList = null;
    protected VA adapter = null;

    private ReadWriteLock dummyLock = new ReentrantReadWriteLock();

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public GridFragment() {
        super();
    }

    protected void bindGridView(GridView view) {
        view.setAdapter(adapter);
    }

    public void reloadData() {
        Lock writeLock = dummyLock.writeLock();
        writeLock.lock();
        try {
            dummyList.reloadData();
            msgHandler.sendMessage(new Message());
        } finally {
            writeLock.unlock();;
        }
    }

    @SuppressLint("HandlerLeak")
    private final Handler msgHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Lock readLock = dummyLock.readLock();
            readLock.lock();
            try {
                adapter.notifyDataSetChanged();
            } finally {
                readLock.unlock();
            }
        }
    };
}
