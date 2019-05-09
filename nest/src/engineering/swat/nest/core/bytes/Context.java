package engineering.swat.nest.core.bytes;

import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;

public class Context {

	private final ByteOrder endianness;
	private final Charset encoding;
	private final ParseLogTarget logTarget;
	private final Sign sign;

	private Context(Charset encoding, ByteOrder endianness, ParseLogTarget logTarget, Sign sign) {
		this.encoding = encoding;
		this.endianness = endianness;
		this.logTarget = logTarget;
		this.sign = sign;
	}
	
	public static Context DEFAULT = new Context(StandardCharsets.US_ASCII, ByteOrder.BIG_ENDIAN, ParseLogTarget.SINK, Sign.UNSIGNED);

	public Context setEncoding(Charset encoding) {
		return new Context(encoding, endianness, logTarget, sign);
	}

	public Context setByteOrder(ByteOrder endianness) {
		return new Context(encoding, endianness, logTarget, sign);
	}

	public Context setParseLogTarget(ParseLogTarget logTarget) {
		return new Context(encoding, endianness, logTarget, sign);
    }

	public Context setSign(Sign sign) {
		return new Context(encoding, endianness, logTarget, sign);
    }

	public ByteOrder getByteOrder() {
	    return this.endianness;
	}

	public Charset getEncoding() {
		return encoding;
	}
	
	public Sign getSign() {
		return sign;
	}


	@Override
	public boolean equals(@Nullable Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Context context = (Context) o;
		return endianness.equals(context.endianness) &&
				encoding.equals(context.encoding) && 
				sign == context.sign;
	}

	@Override
	public int hashCode() {
		return Objects.hash(endianness, encoding, sign);
	}

	public void fail(String msg) {
		logTarget.fail(msg);
	}
	public void fail(String msg, @Nullable Object p0) {
		logTarget.fail(msg, p0);
	}

	public void fail(String msg, @Nullable Object p0, @Nullable Object p1) {
		logTarget.fail(msg, p0, p1);
	}

	public void fail(String msg, @Nullable Object p0, @Nullable Object p1, @Nullable Object p2) {
		logTarget.fail(msg, p0, p1, p2);
	}

}
