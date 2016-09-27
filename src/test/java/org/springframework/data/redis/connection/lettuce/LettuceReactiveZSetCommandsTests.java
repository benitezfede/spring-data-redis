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
package org.springframework.data.redis.connection.lettuce;

import static org.hamcrest.core.Is.*;
import static org.junit.Assert.*;

import java.util.Arrays;

import org.hamcrest.collection.IsIterableContainingInOrder;
import org.junit.Test;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.DefaultTuple;

/**
 * @author Christoph Strobl
 */
public class LettuceReactiveZSetCommandsTests extends LettuceReactiveCommandsTestsBase {

	/**
	 * @see DATAREDIS-525
	 */
	@Test
	public void zAddShouldAddValuesWithScores() {
		assertThat(connection.zSetCommands().zAdd(KEY_1_BBUFFER, 3.5D, VALUE_1_BBUFFER).block(), is(1L));
	}

	/**
	 * @see DATAREDIS-525
	 */
	@Test
	public void zRemShouldRemoveValuesFromSet() {

		nativeCommands.zadd(KEY_1, 1D, VALUE_1);
		nativeCommands.zadd(KEY_1, 2D, VALUE_2);
		nativeCommands.zadd(KEY_1, 3D, VALUE_3);

		assertThat(connection.zSetCommands().zRem(KEY_1_BBUFFER, Arrays.asList(VALUE_1_BBUFFER, VALUE_3_BBUFFER)).block(),
				is(2L));
	}

	/**
	 * @see DATAREDIS-525
	 */
	@Test
	public void zIncrByShouldInreaseAndReturnScore() {

		nativeCommands.zadd(KEY_1, 1D, VALUE_1);

		assertThat(connection.zSetCommands().zIncrBy(KEY_1_BBUFFER, 3.5D, VALUE_1_BBUFFER).block(), is(4.5D));
	}

	/**
	 * @see DATAREDIS-525
	 */
	@Test
	public void zRankShouldReturnIndexCorrectly() {

		nativeCommands.zadd(KEY_1, 1D, VALUE_1);
		nativeCommands.zadd(KEY_1, 2D, VALUE_2);
		nativeCommands.zadd(KEY_1, 3D, VALUE_3);

		assertThat(connection.zSetCommands().zRank(KEY_1_BBUFFER, VALUE_3_BBUFFER).block(), is(2L));
	}

	/**
	 * @see DATAREDIS-525
	 */
	@Test
	public void zRevRankShouldReturnIndexCorrectly() {

		nativeCommands.zadd(KEY_1, 1D, VALUE_1);
		nativeCommands.zadd(KEY_1, 2D, VALUE_2);
		nativeCommands.zadd(KEY_1, 3D, VALUE_3);

		assertThat(connection.zSetCommands().zRevRank(KEY_1_BBUFFER, VALUE_3_BBUFFER).block(), is(0L));
	}

	/**
	 * @see DATAREDIS-525
	 */
	@Test
	public void zRangeShouldReturnValuesCorrectly() {

		nativeCommands.zadd(KEY_1, 1D, VALUE_1);
		nativeCommands.zadd(KEY_1, 2D, VALUE_2);
		nativeCommands.zadd(KEY_1, 3D, VALUE_3);

		assertThat(connection.zSetCommands().zRange(KEY_1_BBUFFER, new Range<Long>(1L, 2L)).block(),
				IsIterableContainingInOrder.contains(VALUE_2_BBUFFER, VALUE_3_BBUFFER));
	}

	/**
	 * @see DATAREDIS-525
	 */
	@Test
	public void zRangeWithScoreShouldReturnTuplesCorrectly() {

		nativeCommands.zadd(KEY_1, 1D, VALUE_1);
		nativeCommands.zadd(KEY_1, 2D, VALUE_2);
		nativeCommands.zadd(KEY_1, 3D, VALUE_3);

		assertThat(connection.zSetCommands().zRangeWithScores(KEY_1_BBUFFER, new Range<Long>(1L, 2L)).block(),
				IsIterableContainingInOrder.contains(new DefaultTuple(VALUE_2_BBUFFER.array(), 2D),
						new DefaultTuple(VALUE_3_BBUFFER.array(), 3D)));
	}

	/**
	 * @see DATAREDIS-525
	 */
	@Test
	public void zRevRangeShouldReturnValuesCorrectly() {

		nativeCommands.zadd(KEY_1, 1D, VALUE_1);
		nativeCommands.zadd(KEY_1, 2D, VALUE_2);
		nativeCommands.zadd(KEY_1, 3D, VALUE_3);

		assertThat(connection.zSetCommands().zRevRange(KEY_1_BBUFFER, new Range<Long>(1L, 2L)).block(),
				IsIterableContainingInOrder.contains(VALUE_2_BBUFFER, VALUE_1_BBUFFER));
	}

	/**
	 * @see DATAREDIS-525
	 */
	@Test
	public void zRevRangeWithScoreShouldReturnTuplesCorrectly() {

		nativeCommands.zadd(KEY_1, 1D, VALUE_1);
		nativeCommands.zadd(KEY_1, 2D, VALUE_2);
		nativeCommands.zadd(KEY_1, 3D, VALUE_3);

		assertThat(connection.zSetCommands().zRevRangeWithScores(KEY_1_BBUFFER, new Range<Long>(1L, 2L)).block(),
				IsIterableContainingInOrder.contains(new DefaultTuple(VALUE_2_BBUFFER.array(), 2D),
						new DefaultTuple(VALUE_1_BBUFFER.array(), 1D)));
	}

}
