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
import java.util.function.Supplier;

import org.reactivestreams.Publisher;
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

			Assert.notNull(key, "Key must not be null!");
			Assert.notNull(value, "Value must not be null!");

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

			Assert.notNull(key, "Key must not be null!");
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

			Assert.notNull(key, "Key must not be null!");
			Assert.notNull(value, "Value must not be null!");

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

			Assert.notNull(key, "Key must not be null!");
			Assert.notNull(value, "Value must not be null!");
			Assert.notNull(expiration, "Expiration must not be null!");
			Assert.notNull(option, "Option must not be null!");

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

			Assert.notNull(keys, "Keys must not be null!");
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

			Assert.notNull(key, "Keys must not be null!");
			Assert.notNull(value, "Keys must not be null!");

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

			Assert.notNull(key, "Keys must not be null!");
			Assert.notNull(value, "Keys must not be null!");
			Assert.notNull(key, "ExpireTimeout must not be null!");

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

			Assert.notNull(key, "Keys must not be null!");
			Assert.notNull(value, "Keys must not be null!");
			Assert.notNull(key, "ExpireTimeout must not be null!");

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

	}

	/**
	 * @author Christoph Strobl
	 * @since 2.0
	 */
	static interface ReactiveKeyCommands {

		default Mono<Boolean> exists(ByteBuffer key) {

			Assert.notNull(key, "Key must not be null!");
			return exists(Mono.just(key)).next().map(BooleanResponse::getOutput);
		}

		Flux<BooleanResponse<ByteBuffer>> exists(Publisher<ByteBuffer> keys);

		/**
		 * Delete {@literal key}.
		 * 
		 * @param key must not be {@literal null}.
		 * @return
		 */
		default Mono<Long> del(ByteBuffer key) {

			Assert.notNull(key, "Key must not be null!");
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

			Assert.notEmpty(keys, "Keys must not be empty or null!");
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
