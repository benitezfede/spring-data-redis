/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.redis.connection;

import java.io.Closeable;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.reactivestreams.Publisher;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.RedisStringCommands.BitOperation;
import org.springframework.data.redis.connection.RedisStringCommands.SetOption;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.util.Assert;

import lombok.Data;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author Christoph Strobl
 * @since 2.0
 */
public interface ReactiveRedisConnection extends Closeable {

	/**
	 * Get {@link ReactiveKeyCommands}.
	 * 
	 * @return never {@literal null}
	 */
	ReactiveKeyCommands keyCommands();

	/**
	 * Get {@link ReactiveStringCommands}.
	 * 
	 * @return never {@literal null}
	 */
	ReactiveStringCommands stringCommands();

	/**
	 */
	ReactiveNumberCommands numberCommands();

	@Data
	public static class CommandResponse<I, O> {

		private final I input;
		private final O output;
	}

	public static class BooleanResponse<I> extends CommandResponse<I, Boolean> {

		public BooleanResponse(I input, Boolean output) {
			super(input, output);
		}
	}

	public static class ByteBufferResponse<I> extends CommandResponse<I, ByteBuffer> {

		public ByteBufferResponse(I input, ByteBuffer output) {
			super(input, output);
		}
	}

	public static class MultiValueResponse<I, O> extends CommandResponse<I, List<O>> {

		public MultiValueResponse(I input, List<O> output) {
			super(input, output);
		}
	}

	public static class NumericResponse<I, O extends Number> extends CommandResponse<I, O> {

		public NumericResponse(I input, O output) {
			super(input, output);
		}
	}

	/**
	 * @author Christoph Strobl
	 * @since 2.0
	 */
	@Data
	static class KeyValue {

		final ByteBuffer key;
		final ByteBuffer value;

		public byte[] keyAsBytes() {
			return key.array();
		}

		public byte[] valueAsBytes() {
			return value.array();
		}
	}

	/**
	 * @author Christoph Strobl
	 * @since 2.0
	 */
	static interface ReactiveStringCommands {

		/**
		 * Set {@literal value} for {@literal key}.
		 * 
		 * @param key must not be {@literal null}.
		 * @param value must not be {@literal null}.
		 * @return
		 */
		default Mono<Boolean> set(ByteBuffer key, ByteBuffer value) {

			try {
				Assert.notNull(key, "Key must not be null!");
				Assert.notNull(value, "Value must not be null!");
			} catch (IllegalArgumentException e) {
				return Mono.error(e);
			}

			return set(Mono.just(new KeyValue(key, value))).next().map(BooleanResponse::getOutput);
		}

		/**
		 * Set each and every {@link KeyValue} item separately.
		 * 
		 * @param values must not be {@literal null}.
		 * @return {@link Flux} of {@link SetResponse} holding the {@link KeyValue} pair to set along with the command
		 *         result.
		 */
		Flux<BooleanResponse<KeyValue>> set(Publisher<KeyValue> values);

		/**
		 * Get single element stored at {@literal key}.
		 * 
		 * @param key must not be {@literal null}.
		 * @return empty {@link ByteBuffer} in case {@literal key} does not exist.
		 */
		default Mono<ByteBuffer> get(ByteBuffer key) {

			try {
				Assert.notNull(key, "Key must not be null!");
			} catch (IllegalArgumentException e) {
				return Mono.error(e);
			}

			return get(Mono.just(key)).next().map((result) -> result.getOutput());
		}

		/**
		 * Get elements one by one.
		 * 
		 * @param keys must not be {@literal null}.
		 * @return {@link Flux} of {@link GetResponse} holding the {@literal key} to get along with the value retrieved.
		 */
		Flux<ByteBufferResponse<ByteBuffer>> get(Publisher<ByteBuffer> keys);

		/**
		 * Set {@literal value} for {@literal key} and return the existing value.
		 * 
		 * @param key must not be {@literal null}.
		 * @param value must not be {@literal null}.
		 * @return
		 */
		default Mono<ByteBuffer> getSet(ByteBuffer key, ByteBuffer value) {

			try {
				Assert.notNull(key, "Key must not be null!");
				Assert.notNull(value, "Value must not be null!");
			} catch (IllegalArgumentException e) {
				return Mono.error(e);
			}

			return getSet(Mono.just(new KeyValue(key, value))).next().map(ByteBufferResponse::getOutput);
		}

		/**
		 * Set {@literal value} for {@literal key} and return the existing value one by one.
		 * 
		 * @param key must not be {@literal null}.
		 * @param value must not be {@literal null}.
		 * @return {@link Flux} of {@link GetSetResponse} holding the {@link KeyValue} pair to set along with the previously
		 *         existing value.
		 */
		Flux<ByteBufferResponse<KeyValue>> getSet(Publisher<KeyValue> values);

		/**
		 * Set {@literal value} for {@literal key} with {@literal expiration} and {@literal options}.
		 * 
		 * @param key must not be {@literal null}.
		 * @param value must not be {@literal null}.
		 * @param expiration must not be {@literal null}.
		 * @param option must not be {@literal null}.
		 * @return
		 */
		default Mono<Boolean> set(ByteBuffer key, ByteBuffer value, Expiration expiration, SetOption option) {

			try {
				Assert.notNull(key, "Key must not be null!");
				Assert.notNull(value, "Value must not be null!");
				Assert.notNull(expiration, "Expiration must not be null!");
				Assert.notNull(option, "Option must not be null!");
			} catch (IllegalArgumentException e) {
				return Mono.error(e);
			}

			return set(Mono.just(new KeyValue(key, value)), () -> expiration, () -> option).next()
					.map(BooleanResponse::getOutput);
		}

		/**
		 * Set {@literal value} for {@literal key} with {@literal expiration} and {@literal options} one by one.
		 * 
		 * @param values must not be {@literal null}.
		 * @param expiration must not be {@literal null}.
		 * @param option must not be {@literal null}.
		 * @return {@link Flux} of {@link SetResponse} holding the {@link KeyValue} pair to set along with the command
		 *         result.
		 */
		Flux<BooleanResponse<KeyValue>> set(Publisher<KeyValue> values, Supplier<Expiration> expiration,
				Supplier<SetOption> option);

		/**
		 * Get multiple values in one batch.
		 * 
		 * @param keys must not be {@literal null}.
		 * @return
		 */
		default Mono<List<ByteBuffer>> mGet(List<ByteBuffer> keys) {

			try {
				Assert.notNull(keys, "Keys must not be null!");
			} catch (IllegalArgumentException e) {
				return Mono.error(e);
			}

			return mGet(Mono.just(keys)).next().map(MultiValueResponse::getOutput);
		}

		/**
		 * Get multiple values at in batches.
		 * 
		 * @param keys must not be {@literal null}.
		 * @return
		 */
		Flux<MultiValueResponse<List<ByteBuffer>, ByteBuffer>> mGet(Publisher<List<ByteBuffer>> keysets);

		/**
		 * Set {@code value} for {@code key}, only if {@code key} does not exist.
		 *
		 * @param key must not be {@literal null}.
		 * @param value must not be {@literal null}.
		 * @return
		 */
		default Mono<Boolean> setNX(ByteBuffer key, ByteBuffer value) {

			try {
				Assert.notNull(key, "Keys must not be null!");
				Assert.notNull(value, "Keys must not be null!");
			} catch (IllegalArgumentException e) {
				return Mono.error(e);
			}

			return setNX(Mono.just(new KeyValue(key, value))).next().map(BooleanResponse::getOutput);
		}

		/**
		 * Set {@code key value} pairs, only if {@code key} does not exist.
		 *
		 * @param values must not be {@literal null}.
		 * @return
		 */
		Flux<BooleanResponse<KeyValue>> setNX(Publisher<KeyValue> values);

		/**
		 * Set {@code key value} pair and {@link Expiration}.
		 *
		 * @param key must not be {@literal null}.
		 * @param value must not be {@literal null}.
		 * @param expireTimeout must not be {@literal null}.
		 * @return
		 */
		default Mono<Boolean> setEX(ByteBuffer key, ByteBuffer value, Expiration expireTimeout) {

			try {
				Assert.notNull(key, "Keys must not be null!");
				Assert.notNull(value, "Keys must not be null!");
				Assert.notNull(key, "ExpireTimeout must not be null!");
			} catch (IllegalArgumentException e) {
				return Mono.error(e);
			}

			return setEX(Mono.just(new KeyValue(key, value)), () -> expireTimeout).next().map(BooleanResponse::getOutput);
		}

		/**
		 * Set {@code key value} pairs and {@link Expiration}.
		 *
		 * @param source must not be {@literal null}.
		 * @param expireTimeout must not be {@literal null}.
		 * @return
		 */
		Flux<BooleanResponse<KeyValue>> setEX(Publisher<KeyValue> source, Supplier<Expiration> expireTimeout);

		/**
		 * Set {@code key value} pair and {@link Expiration}.
		 *
		 * @param key must not be {@literal null}.
		 * @param value must not be {@literal null}.
		 * @param expireTimeout must not be {@literal null}.
		 * @return
		 */
		default Mono<Boolean> pSetEX(ByteBuffer key, ByteBuffer value, Expiration expireTimeout) {

			try {
				Assert.notNull(key, "Key must not be null!");
				Assert.notNull(value, "Value must not be null!");
				Assert.notNull(key, "ExpireTimeout must not be null!");
			} catch (IllegalArgumentException e) {
				return Mono.error(e);
			}

			return pSetEX(Mono.just(new KeyValue(key, value)), () -> expireTimeout).next().map(BooleanResponse::getOutput);
		}

		/**
		 * Set {@code key value} pairs and {@link Expiration}.
		 *
		 * @param source must not be {@literal null}.
		 * @param expireTimeout must not be {@literal null}.
		 * @return
		 */
		Flux<BooleanResponse<KeyValue>> pSetEX(Publisher<KeyValue> source, Supplier<Expiration> expireTimeout);

		/**
		 * Set multiple keys to multiple values using key-value pairs provided in {@code tuple}.
		 *
		 * @param tuples must not be {@literal null}.
		 * @return
		 */
		default Mono<Boolean> mSet(Map<ByteBuffer, ByteBuffer> tuples) {

			try {
				Assert.notNull(tuples, "Tuples must not be null!");
			} catch (IllegalArgumentException e) {
				return Mono.error(e);
			}

			return mSet(Flux.just(tuples.entrySet().stream().map(entry -> new KeyValue(entry.getKey(), entry.getValue()))
					.collect(Collectors.toList()))).next().map(BooleanResponse::getOutput);
		}

		/**
		 * Set multiple keys to multiple values using key-value pairs provided in {@code source}.
		 *
		 * @param source must not be {@literal null}.
		 * @return
		 */
		Flux<BooleanResponse<List<KeyValue>>> mSet(Publisher<List<KeyValue>> source);

		/**
		 * Set multiple keys to multiple values using key-value pairs provided in {@code tuples} only if the provided key
		 * does not exist.
		 *
		 * @param tuples must not be {@literal null}.
		 * @return
		 */
		default Mono<Boolean> mSetNX(Map<ByteBuffer, ByteBuffer> tuples) {

			try {
				Assert.notNull(tuples, "Tuples must not be null!");
			} catch (IllegalArgumentException e) {
				return Mono.error(e);
			}

			return mSetNX(Flux.just(tuples.entrySet().stream().map(entry -> new KeyValue(entry.getKey(), entry.getValue()))
					.collect(Collectors.toList()))).next().map(BooleanResponse::getOutput);
		}

		/**
		 * Set multiple keys to multiple values using key-value pairs provided in {@code tuples} only if the provided key
		 * does not exist.
		 *
		 * @param source must not be {@literal null}.
		 * @return
		 */
		Flux<BooleanResponse<List<KeyValue>>> mSetNX(Publisher<List<KeyValue>> source);

		/**
		 * Append a {@code value} to {@code key}.
		 *
		 * @param key must not be {@literal null}.
		 * @param value must not be {@literal null}.
		 * @return
		 */
		default Mono<Long> append(ByteBuffer key, ByteBuffer value) {

			try {
				Assert.notNull(key, "Key must not be null!");
				Assert.notNull(value, "Value must not be null!");
			} catch (IllegalArgumentException e) {
				return Mono.error(e);
			}

			return append(Mono.just(new KeyValue(key, value))).next().map(NumericResponse::getOutput);
		}

		/**
		 * Append a {@link KeyValue#value} to {@link KeyValue#key}
		 *
		 * @param source must not be {@literal null}.
		 * @return
		 */
		Flux<NumericResponse<KeyValue, Long>> append(Publisher<KeyValue> source);

		/**
		 * Get a substring of value of {@code key} between {@code begin} and {@code end}.
		 *
		 * @param key must not be {@literal null}.
		 * @param begin
		 * @param end
		 * @return
		 */
		default Mono<ByteBuffer> getRange(ByteBuffer key, long begin, long end) {

			try {
				Assert.notNull(key, "Key must not be null!");
			} catch (IllegalArgumentException e) {
				return Mono.error(e);
			}

			return getRange(Mono.just(key), () -> begin, () -> end).next().map(ByteBufferResponse::getOutput);
		}

		/**
		 * Get a substring of value of {@code key} between {@code begin} and {@code end}.
		 *
		 * @param keys must not be {@literal null}.
		 * @param begin
		 * @param end
		 * @return
		 */
		Flux<ByteBufferResponse<ByteBuffer>> getRange(Publisher<ByteBuffer> keys, Supplier<Long> begin, Supplier<Long> end);

		/**
		 * Overwrite parts of {@code key} starting at the specified {@code offset} with given {@code value}.
		 *
		 * @param key must not be {@literal null}.
		 * @param value must not be {@literal null}.
		 * @param offset
		 * @return
		 */
		default Mono<Long> setRange(ByteBuffer key, ByteBuffer value, long offset) {

			try {
				Assert.notNull(key, "Key must not be null!");
				Assert.notNull(value, "Value must not be null!");
			} catch (IllegalArgumentException e) {
				return Mono.error(e);
			}

			return setRange(Mono.just(new KeyValue(key, value)), () -> Long.valueOf(offset)).next()
					.map(NumericResponse::getOutput);
		}

		/**
		 * Overwrite parts of {@link KeyValue#key} starting at the specified {@code offset} with given
		 * {@link KeyValue#value}.
		 *
		 * @param keys must not be {@literal null}.
		 * @param offset must not be {@literal null}.
		 * @return
		 */
		Flux<NumericResponse<KeyValue, Long>> setRange(Publisher<KeyValue> keys, Supplier<Long> offset);

		/**
		 * Get the bit value at {@code offset} of value at {@code key}.
		 *
		 * @param key must not be {@literal null}.
		 * @param offset
		 * @return
		 */
		default Mono<Boolean> getBit(ByteBuffer key, long offset) {

			try {
				Assert.notNull(key, "Key must not be null!");
			} catch (IllegalArgumentException e) {
				return Mono.error(e);
			}

			return getBit(Mono.just(key), () -> Long.valueOf(offset)).next().map(BooleanResponse::getOutput);
		}

		/**
		 * Get the bit value at {@code offset} of value at {@code key}.
		 *
		 * @param keys must not be {@literal null}.
		 * @param offset must not be {@literal null}.
		 * @return
		 */
		Flux<BooleanResponse<ByteBuffer>> getBit(Publisher<ByteBuffer> keys, Supplier<Long> offset);

		/**
		 * Sets the bit at {@code offset} in value stored at {@code key} and return the original value.
		 *
		 * @param key must not be {@literal null}.
		 * @param offset
		 * @param value
		 * @return
		 */
		default Mono<Boolean> setBit(ByteBuffer key, long offset, boolean value) {

			try {
				Assert.notNull(key, "Key must not be null!");
			} catch (IllegalArgumentException e) {
				return Mono.error(e);
			}

			return setBit(Mono.just(key), () -> offset, () -> value).next().map(BooleanResponse::getOutput);
		}

		/**
		 * Sets the bit at {@code offset} in value stored at {@code key} and return the original value.
		 *
		 * @param keys must not be {@literal null}.
		 * @param offset must not be {@literal null}.
		 * @param value must not be {@literal null}.
		 * @return
		 */
		Flux<BooleanResponse<ByteBuffer>> setBit(Publisher<ByteBuffer> keys, Supplier<Long> offset,
				Supplier<Boolean> value);

		/**
		 * Count the number of set bits (population counting) in value stored at {@code key}.
		 *
		 * @param key must not be {@literal null}.
		 * @return
		 */
		default Mono<Long> bitCount(ByteBuffer key) {

			try {
				Assert.notNull(key, "Key must not be null");
			} catch (IllegalArgumentException e) {
				return Mono.error(e);
			}

			return bitCount(Mono.just(key)).next().map(NumericResponse::getOutput);
		}

		/**
		 * Count the number of set bits (population counting) in value stored at {@code key}.
		 *
		 * @param keys must not be {@literal null}.
		 * @return
		 */
		Flux<NumericResponse<ByteBuffer, Long>> bitCount(Publisher<ByteBuffer> keys);

		/**
		 * Count the number of set bits (population counting) of value stored at {@code key} between {@code begin} and
		 * {@code end}.
		 *
		 * @param key must not be {@literal null}.
		 * @param begin
		 * @param end
		 * @return
		 */
		default Mono<Long> bitCount(ByteBuffer key, long begin, long end) {

			try {
				Assert.notNull(key, "Key must not be null");
			} catch (IllegalArgumentException e) {
				return Mono.error(e);
			}

			return bitCount(Mono.just(key), () -> new Range<>(begin, end)).next().map(NumericResponse::getOutput);
		}

		/**
		 * Count the number of set bits (population counting) of value stored at {@code key} between {@code begin} and
		 * {@code end}.
		 *
		 * @param keys must not be {@literal null}.
		 * @param begin must not be {@literal null}.
		 * @param end must not be {@literal null}.
		 * @return
		 */
		Flux<NumericResponse<ByteBuffer, Long>> bitCount(Publisher<ByteBuffer> keys, Supplier<Range<Long>> range);

		/**
		 * Perform bitwise operations between strings.
		 *
		 * @param keys must not be {@literal null}.
		 * @param bitOp must not be {@literal null}.
		 * @param destination must not be {@literal null}.
		 * @return
		 */
		default Mono<Long> bitOp(List<ByteBuffer> keys, BitOperation bitOp, ByteBuffer destination) {

			try {
				Assert.notNull(keys, "keys must not be null");
			} catch (IllegalArgumentException e) {
				return Mono.error(e);
			}

			return bitOp(Mono.just(keys), () -> bitOp, () -> destination).next().map(NumericResponse::getOutput);
		}

		/**
		 * Perform bitwise operations between strings.
		 *
		 * @param keys must not be {@literal null}.
		 * @param bitOp must not be {@literal null}.
		 * @param destination must not be {@literal null}.
		 * @return
		 */
		Flux<NumericResponse<List<ByteBuffer>, Long>> bitOp(Publisher<List<ByteBuffer>> keys, Supplier<BitOperation> bitOp,
				Supplier<ByteBuffer> destination);

		/**
		 * Get the length of the value stored at {@code key}.
		 *
		 * @param key must not be {@literal null}.
		 * @return
		 */
		default Mono<Long> strLen(ByteBuffer key) {

			try {
				Assert.notNull(key, "key must not be null");
			} catch (IllegalArgumentException e) {
				return Mono.error(e);
			}

			return strLen(Mono.just(key)).next().map(NumericResponse::getOutput);
		}

		/**
		 * Get the length of the value stored at {@code key}.
		 *
		 * @param keys must not be {@literal null}.
		 * @return
		 */
		Flux<NumericResponse<ByteBuffer, Long>> strLen(Publisher<ByteBuffer> keys);
	}

	/**
	 * @author Christoph Strobl
	 * @since 2.0
	 */
	static interface ReactiveNumberCommands {

		/**
		 * Increment value of {@code key} by 1.
		 *
		 * @param key must not be {@literal null}.
		 * @return
		 */
		default Mono<Long> incr(ByteBuffer key) {

			try {
				Assert.notNull(key, "key must not be null");
			} catch (IllegalArgumentException e) {
				return Mono.error(e);
			}

			return incr(Mono.just(key)).next().map(NumericResponse::getOutput);
		}

		/**
		 * Increment value of {@code key} by 1.
		 *
		 * @param keys must not be {@literal null}.
		 * @return
		 */
		Flux<NumericResponse<ByteBuffer, Long>> incr(Publisher<ByteBuffer> keys);

		/**
		 * Increment value of {@code key} by {@code value}.
		 *
		 * @param key must not be {@literal null}.
		 * @param value must not be {@literal null}.
		 * @return
		 */
		default <T extends Number> Mono<T> incrBy(ByteBuffer key, T value) {

			try {
				Assert.notNull(key, "key must not be null");
				Assert.notNull(value, "value must not be null");
			} catch (IllegalArgumentException e) {
				return Mono.error(e);
			}

			return incrBy(Mono.just(key), () -> value).next().map(NumericResponse::getOutput);
		}

		/**
		 * Increment value of {@code key} by {@code value}.
		 *
		 * @param keys must not be {@literal null}.
		 * @param value must not be {@literal null}.
		 * @return
		 */
		<T extends Number> Flux<NumericResponse<ByteBuffer, T>> incrBy(Publisher<ByteBuffer> keys, Supplier<T> value);

		/**
		 * Decrement value of {@code key} by 1.
		 *
		 * @param key must not be {@literal null}.
		 * @return
		 */
		default Mono<Long> decr(ByteBuffer key) {

			try {
				Assert.notNull(key, "key must not be null");
			} catch (IllegalArgumentException e) {
				return Mono.error(e);
			}

			return decr(Mono.just(key)).next().map(NumericResponse::getOutput);
		}

		/**
		 * Decrement value of {@code key} by 1.
		 *
		 * @param keys must not be {@literal null}.
		 * @return
		 */
		Flux<NumericResponse<ByteBuffer, Long>> decr(Publisher<ByteBuffer> keys);

		/**
		 * Decrement value of {@code key} by {@code value}.
		 *
		 * @param key must not be {@literal null}.
		 * @param value must not be {@literal null}.
		 * @return
		 */
		default <T extends Number> Mono<T> decrBy(ByteBuffer key, T value) {

			try {
				Assert.notNull(key, "key must not be null");
				Assert.notNull(value, "value must not be null");
			} catch (IllegalArgumentException e) {
				return Mono.error(e);
			}

			return decrBy(Mono.just(key), () -> value).next().map(NumericResponse::getOutput);
		}

		/**
		 * Decrement value of {@code key} by {@code value}.
		 *
		 * @param keys must not be {@literal null}.
		 * @param value must not be {@literal null}.
		 * @return
		 */
		<T extends Number> Flux<NumericResponse<ByteBuffer, T>> decrBy(Publisher<ByteBuffer> keys, Supplier<T> value);

	}

	/**
	 * @author Christoph Strobl
	 * @since 2.0
	 */
	static interface ReactiveKeyCommands {

		/**
		 * Determine if given {@code key} exists.
		 *
		 * @param key must not be {@literal null}.
		 * @return
		 */
		default Mono<Boolean> exists(ByteBuffer key) {

			try {
				Assert.notNull(key, "Key must not be null!");
			} catch (IllegalArgumentException e) {
				return Mono.error(e);
			}

			return exists(Mono.just(key)).next().map(BooleanResponse::getOutput);
		}

		/**
		 * Determine if given {@code key} exists.
		 *
		 * @param keys must not be {@literal null}.
		 * @return
		 */
		Flux<BooleanResponse<ByteBuffer>> exists(Publisher<ByteBuffer> keys);

		/**
		 * Determine the type stored at {@code key}.
		 *
		 * @param key must not be {@literal null}.
		 * @return
		 */
		default Mono<DataType> type(ByteBuffer key) {

			try {
				Assert.notNull(key, "key must not be null");
			} catch (IllegalArgumentException e) {
				return Mono.error(e);
			}

			return type(Mono.just(key)).next().map(CommandResponse::getOutput);
		}

		/**
		 * Determine the type stored at {@code key}.
		 *
		 * @param keys must not be {@literal null}.
		 * @return
		 */
		Flux<CommandResponse<ByteBuffer, DataType>> type(Publisher<ByteBuffer> keys);

		/**
		 * Find all keys matching the given {@code pattern}.
		 *
		 * @param pattern must not be {@literal null}.
		 * @return
		 */
		default Mono<List<ByteBuffer>> keys(ByteBuffer pattern) {

			try {
				Assert.notNull(pattern, "pattern must not be null");
			} catch (IllegalArgumentException e) {
				return Mono.error(e);
			}

			return keys(Mono.just(pattern)).next().map(MultiValueResponse::getOutput);
		}

		/**
		 * Return a random key from the keyspace.
		 *
		 * @return
		 */
		Mono<ByteBuffer> randomKey();

		/**
		 * Find all keys matching the given {@code pattern}.
		 *
		 * @param patterns must not be {@literal null}.
		 * @return
		 */
		Flux<MultiValueResponse<ByteBuffer, ByteBuffer>> keys(Publisher<ByteBuffer> patterns);

		/**
		 * Rename key {@code oleName} to {@code newName}.
		 *
		 * @param key must not be {@literal null}.
		 * @param newName must not be {@literal null}.
		 * @return
		 */
		default Mono<Boolean> rename(ByteBuffer key, ByteBuffer newName) {

			try {
				Assert.notNull(key, "key must not be null");
			} catch (IllegalArgumentException e) {
				return Mono.error(e);
			}

			return rename(Mono.just(key), () -> newName).next().map(BooleanResponse::getOutput);
		}

		/**
		 * Rename key {@code oleName} to {@code newName}.
		 *
		 * @param keys must not be {@literal null}.
		 * @param newName must not be {@literal null}.
		 * @return
		 */
		Flux<BooleanResponse<ByteBuffer>> rename(Publisher<ByteBuffer> keys, Supplier<ByteBuffer> newName);

		/**
		 * Rename key {@code oleName} to {@code newName} only if {@code newName} does not exist.
		 *
		 * @param key must not be {@literal null}.
		 * @param newName must not be {@literal null}.
		 * @return
		 */
		default Mono<Boolean> renameNX(ByteBuffer key, ByteBuffer newName) {

			try {
				Assert.notNull(key, "key must not be null");
			} catch (IllegalArgumentException e) {
				return Mono.error(e);
			}

			return renameNX(Mono.just(key), () -> newName).next().map(BooleanResponse::getOutput);
		}

		/**
		 * Rename key {@code oleName} to {@code newName} only if {@code newName} does not exist.
		 *
		 * @param keys must not be {@literal null}.
		 * @param newName must not be {@literal null}.
		 * @return
		 */
		Flux<BooleanResponse<ByteBuffer>> renameNX(Publisher<ByteBuffer> keys, Supplier<ByteBuffer> newName);

		/**
		 * Delete {@literal key}.
		 * 
		 * @param key must not be {@literal null}.
		 * @return
		 */
		default Mono<Long> del(ByteBuffer key) {

			try {
				Assert.notNull(key, "Key must not be null!");
			} catch (IllegalArgumentException e) {
				return Mono.error(e);
			}

			return del(Mono.just(key)).next().map(NumericResponse::getOutput);
		}

		/**
		 * Delete {@literal keys} one by one.
		 * 
		 * @param keys must not be {@literal null}.
		 * @return {@link Flux} of {@link DelResponse} holding the {@literal key} removed along with the deletion result.
		 */
		Flux<NumericResponse<ByteBuffer, Long>> del(Publisher<ByteBuffer> keys);

		/**
		 * Delete multiple {@literal keys} one in one batch.
		 * 
		 * @param keys must not be {@literal null}.
		 * @return
		 */
		default Mono<Long> mDel(List<ByteBuffer> keys) {

			try {
				Assert.notEmpty(keys, "Keys must not be empty or null!");
			} catch (IllegalArgumentException e) {
				return Mono.error(e);
			}

			return mDel(Mono.just(keys)).next().map(NumericResponse::getOutput);
		}

		/**
		 * Delete multiple {@literal keys} in batches.
		 * 
		 * @param keys must not be {@literal null}.
		 * @return {@link Flux} of {@link MDelResponse} holding the {@literal keys} removed along with the deletion result.
		 */
		Flux<NumericResponse<List<ByteBuffer>, Long>> mDel(Publisher<List<ByteBuffer>> keys);
	}
}
