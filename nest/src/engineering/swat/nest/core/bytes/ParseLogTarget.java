package engineering.swat.nest.core.bytes;

import org.checkerframework.checker.nullness.qual.Nullable;

public interface ParseLogTarget {


    void fail(String msg);
    void fail(String msg, @Nullable Object p0);
    void fail(String msg, @Nullable Object p0, @Nullable Object p1);
    void fail(String msg, @Nullable Object p0, @Nullable Object p1, @Nullable Object p2);

    void trace(String msg);
    void trace(String msg, @Nullable Object p0);
    void trace(String msg, @Nullable Object p0, @Nullable Object p1);
    void trace(String msg, @Nullable Object p0, @Nullable Object p1, @Nullable Object p2);

    ParseLogTarget SINK = new ParseLogTarget() {
        @Override
        public void fail(String msg) {
        }

        @Override
        public void fail(String msg, @Nullable Object p0) {
        }

        @Override
        public void fail(String msg, @Nullable Object p0, @Nullable Object p1) {
        }

        @Override
        public void fail(String msg, @Nullable Object p0, @Nullable Object p1, @Nullable Object p2) {
        }

        @Override
        public void trace(String msg) {
        }

        @Override
        public void trace(String msg, @Nullable Object p0) {
        }

        @Override
        public void trace(String msg, @Nullable Object p0, @Nullable Object p1) {
        }

        @Override
        public void trace(String msg, @Nullable Object p0, @Nullable Object p1, @Nullable Object p2) {
        }
    } ;
}
