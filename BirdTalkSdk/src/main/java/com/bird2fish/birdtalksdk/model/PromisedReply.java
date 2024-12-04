package com.bird2fish.birdtalksdk.model;

import android.util.Log;

import java.util.concurrent.CountDownLatch;

/**
 * 一个非常简单的可链式 Promise 类。它没有执行功能，必须通过外部调用 resolve 或 reject 来解决。
 * 一旦被解决或拒绝，它将调用监听器的 onSuccess 或 onFailure。根据处理程序返回或抛出的结果，它将更新链中的下一个 Promise：
 * 要么立即解决/拒绝它，要么让它和处理程序返回的 Promise 一起解决/拒绝。
 *
 * 使用方法：
 *
 * 创建一个 PromisedReply P1，使用 thenApply 方法分配 onSuccess/onFailure 监听器。thenApply 返回另一个 P2 Promise（mNextPromise），
 * 然后可以为 P2 分配自己的监听器。
 *
 * 或者，可以使用阻塞方法 getResult。它会阻塞，直到 Promise 被解决或拒绝。
 *
 * Promise 可以通过使用适当的构造函数，在 WAITING 或 RESOLVED 状态下创建。
 *
 * onSuccess/onFailure 处理程序将被调用：
 *
 * a. 当 P1 被解决时，如果调用 thenApply 时，Promise 处于 WAITING 状态，
 *    则会在 P1.resolve(T) 时被调用，
 * b. 如果调用 thenApply 时，Promise 已经处于 RESOLVED 或 REJECTED 状态，
 *    则会立即调用 onSuccess 或 onFailure。
 *
 * thenApply 创建并返回一个 Promise P2，该 Promise 将以如下方式解决或拒绝：
 *
 * A. 如果 P1 被解决：
 * 1. 如果 P1.onSuccess 返回一个解决的 Promise P3，则 P2 会立即使用 P3 的结果解决；
 * 2. 如果 P1.onSuccess 返回一个拒绝的 Promise P3，则 P2 会立即使用 P3 的异常拒绝；
 * 3. 如果 P1.onSuccess 返回 null，则 P2 会立即使用 P1 的结果解决；
 * 4. 如果 P1.onSuccess 返回一个未解决的 Promise P3，则 P2 会和 P3 一起解决；
 * 5. 如果 P1.onSuccess 抛出异常，则 P2 会立即使用捕获的异常拒绝；
 * 6. 如果 P1.onSuccess 为 null，则 P2 会立即使用 P1 的结果解决。
 *
 * B. 如果 P1 被拒绝：
 * 1. 如果 P1.onFailure 返回一个解决的 Promise P3，则 P2 会立即使用 P3 的结果解决；
 * 2. 如果 P1.onFailure 返回 null，则 P2 会使用 null 作为结果解决；
 * 3. 如果 P1.onFailure 返回一个未解决的 Promise P3，则 P2 会和 P3 一起解决；
 * 4. 如果 P1.onFailure 抛出异常，则 P2 会立即使用捕获的异常拒绝；
 * 5. 如果 P1.onFailure 为 null，则 P2 会立即使用 P1 的异常拒绝；
 * 5.1 如果 P2.onFailure 为 null，且 P2.mNextPromise 为 null，则会重新抛出异常。
 *
 */
public class PromisedReply<T> {
    private static final String TAG = "PromisedReply";

    private enum State {WAITING, RESOLVED, REJECTED}

    private T mResult = null;
    private Exception mException = null;

    private volatile State mState = State.WAITING;

    private SuccessListener<T> mSuccess = null;
    private FailureListener<T> mFailure = null;

    private PromisedReply<T> mNextPromise = null;

    private final CountDownLatch mDoneSignal;

    /**
     * Create promise in a WAITING state.
     */
    public PromisedReply() {
        mDoneSignal = new CountDownLatch(1);
    }

    /**
     * Create a promise in a RESOLVED state
     *
     * @param result result used for resolution of the promise.
     */
    public PromisedReply(T result) {
        mResult = result;
        mState = State.RESOLVED;
        mDoneSignal = new CountDownLatch(0);
    }

    /**
     * Create a promise in a REJECTED state
     *
     * @param err Exception used for rejecting the promise.
     */
    public <E extends Exception> PromisedReply(E err) {
        mException = err;
        mState = State.REJECTED;
        mDoneSignal = new CountDownLatch(0);
    }

    /**
     * Returns a new PromisedReply that is completed when all of the given PromisedReply complete.
     * @param waitFor promises to wait for.
     * @return new PromisedReply that is completed when all of the given PromisedReply complete.
     */
    public static PromisedReply<Void> allOf(PromisedReply[] waitFor) {
        final PromisedReply<Void> done = new PromisedReply<>();
        // Create a separate thread and wait for all promises to resolve.
        new Thread(() -> {
            for (PromisedReply p : waitFor) {
                try {
                    p.mDoneSignal.await();
                } catch (InterruptedException ignored) {}
            }

            try {
                // If it throws then nothing we can do about it.
                done.resolve(null);
            } catch (Exception ignored) {}
        }).start();
        return done;
    }

    /**
     * Call SuccessListener.onSuccess or FailureListener.onFailure when the
     * promise is resolved or rejected. The call will happen on the thread which
     * called resolve() or reject().
     *
     * @param success called when the promise is resolved
     * @param failure called when the promise is rejected
     * @return promise for chaining
     */
    public PromisedReply<T> thenApply(SuccessListener<T> success, FailureListener<T> failure) {
        synchronized (this) {

            if (mNextPromise != null) {
                throw new IllegalStateException("Multiple calls to thenApply are not supported");
            }

            mSuccess = success;
            mFailure = failure;
            mNextPromise = new PromisedReply<>();
            try {
                switch (mState) {
                    case RESOLVED:
                        callOnSuccess(mResult);
                        break;

                    case REJECTED:
                        callOnFailure(mException);
                        break;

                    case WAITING:
                        break;
                }
            } catch (Exception e) {
                mNextPromise = new PromisedReply<>(e);
            }

            return mNextPromise;
        }
    }

    /**
     * Calls SuccessListener.onSuccess when the promise is resolved. The call will happen on the
     * thread which called resolve().
     *
     * @param success called when the promise is resolved
     * @return promise for chaining
     */
    public PromisedReply<T> thenApply(SuccessListener<T> success) {
        return thenApply(success, null);
    }

    /**
     * Call onFailure when the promise is rejected. The call will happen on the
     * thread which called reject()
     *
     * @param failure called when the promise is rejected
     * @return promise for chaining
     */
    public PromisedReply<T> thenCatch(FailureListener<T> failure) {
        return thenApply(null, failure);
    }

    /**
     * Call FinalListener.onFinally when the promise is completed. The call will happen on the
     * thread which completed the promise: called either resolve() or reject().
     *
     * @param finished called when the promise is completed either way.
     */
    public void thenFinally(final FinalListener finished) {
        thenApply(new SuccessListener<T>() {
            @Override
            public PromisedReply<T> onSuccess(T result) {
                finished.onFinally();
                return null;
            }
        }, new FailureListener<T>() {
            @Override
            public <E extends Exception> PromisedReply<T> onFailure(E err) {
                finished.onFinally();
                return null;
            }
        });
    }

    @SuppressWarnings("WeakerAccess")
    public boolean isResolved() {
        return mState == State.RESOLVED;
    }

    @SuppressWarnings("unused")
    public boolean isRejected() {
        return mState == State.REJECTED;
    }

    @SuppressWarnings({"WeakerAccess"})
    public boolean isDone() {
        return mState == State.RESOLVED || mState == State.REJECTED;
    }


    /**
     * Make this promise resolved.
     *
     * @param result results of resolution.
     * @throws Exception if anything goes wrong during resolution.
     */
    public void resolve(final T result) throws Exception {
        synchronized (this) {
            if (mState == State.WAITING) {
                mState = State.RESOLVED;

                mResult = result;
                try {
                    callOnSuccess(result);
                } finally {
                    mDoneSignal.countDown();
                }
            } else {
                mDoneSignal.countDown();
                throw new IllegalStateException("Promise is already completed");
            }
        }
    }

    /**
     * Make this promise rejected.
     *
     * @param err reason for rejecting this promise.
     * @throws Exception if anything goes wrong during rejection.
     */
    public void reject(final Exception err) throws Exception {
        Log.d(TAG, "REJECTING promise " + this, err);
        synchronized (this) {
            if (mState == State.WAITING) {
                mState = State.REJECTED;

                mException = err;
                try {
                    callOnFailure(err);
                } finally {
                    mDoneSignal.countDown();
                }
            } else {
                mDoneSignal.countDown();
                throw new IllegalStateException("Promise is already completed");
            }
        }
    }

    /**
     * Wait for promise resolution.
     *
     * @return true if the promise was resolved, false otherwise
     * @throws InterruptedException if waiting was interrupted
     */
    public boolean waitResult() throws InterruptedException {
        // Wait for the promise to resolve
        mDoneSignal.await();
        return isResolved();
    }

    /**
     * A blocking call which returns the result of the execution. It will return
     * <b>after</b> thenApply is called. It can be safely called multiple times on
     * the same instance.
     *
     * @return result of the execution (what was passed to {@link #resolve(Object)})
     * @throws Exception if the promise was rejected or waiting was interrupted.
     */
    public T getResult() throws Exception {
        // Wait for the promise to resolve
        mDoneSignal.await();

        switch (mState) {
            case RESOLVED:
                return mResult;

            case REJECTED:
                throw mException;
        }

        throw new IllegalStateException("Promise cannot be in WAITING state");
    }

    private void callOnSuccess(final T result) throws Exception {
        PromisedReply<T> ret;
        try {
            ret = (mSuccess != null ? mSuccess.onSuccess(result) : null);
        } catch (Exception e) {
            handleFailure(e);
            return;
        }
        // If it throws, let it fly.
        handleSuccess(ret);
    }

    private void callOnFailure(final Exception err) throws Exception {
        if (mFailure != null) {
            // Try to recover
            try {
                handleSuccess(mFailure.onFailure(err));
            } catch (Exception ex) {
                handleFailure(ex);
            }
        } else {
            // Pass to the next handler
            handleFailure(err);
        }
    }

    private void handleSuccess(PromisedReply<T> ret) throws Exception {
        if (mNextPromise == null) {
            if (ret != null && ret.mState == State.REJECTED) {
                throw ret.mException;
            }
            return;
        }

        if (ret == null) {
            mNextPromise.resolve(mResult);
        } else if (ret.mState == State.RESOLVED) {
            mNextPromise.resolve(ret.mResult);
        } else if (ret.mState == State.REJECTED) {
            mNextPromise.reject(ret.mException);
        } else {
            // Next promise will be called when ret is completed
            ret.insertNextPromise(mNextPromise);
        }
    }

    private void handleFailure(Exception e) throws Exception {
        if (mNextPromise != null) {
            mNextPromise.reject(e);
        } else {
            throw e;
        }
    }

    private void insertNextPromise(PromisedReply<T> next) {
        synchronized (this) {
            if (mNextPromise != null) {
                next.insertNextPromise(mNextPromise);
            }
            mNextPromise = next;
        }
    }

    public static abstract class SuccessListener<U> {
        /**
         * Callback to execute when the promise is successfully resolved.
         *
         * @param result result of the call.
         * @return new promise to pass to the next handler in the chain or null to use the same result.
         * @throws Exception thrown if handler want to call the next failure handler in chain.
         */
        public abstract PromisedReply<U> onSuccess(U result) throws Exception;
    }

    public static abstract class FailureListener<U> {
        /**
         * Callback to execute when the promise is rejected.
         *
         * @param err Exception which caused promise to fail.
         * @return new promise to pass to the next success handler in the chain.
         * @throws Exception thrown if handler want to call the next failure handler in chain.
         */
        public abstract <E extends Exception> PromisedReply<U> onFailure(E err) throws Exception;
    }

    public static abstract class FinalListener {
        public abstract void onFinally();
    }
}
