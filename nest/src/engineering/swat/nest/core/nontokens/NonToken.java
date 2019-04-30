package engineering.swat.nest.core.nontokens;

import engineering.swat.nest.core.NestValue;
import engineering.swat.nest.core.bytes.ByteSlice;
import java.nio.ByteOrder;

public abstract class NonToken implements NestValue {

    @Override
    public ByteSlice getBytes() {
        return getBytes(ByteOrder.BIG_ENDIAN);
    }

    public abstract ByteSlice getBytes(ByteOrder order);
}
