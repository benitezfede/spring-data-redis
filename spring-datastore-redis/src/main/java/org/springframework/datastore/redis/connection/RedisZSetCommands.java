/*
 * Copyright 2010 the original author or authors.
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

package org.springframework.datastore.redis.connection;

import java.util.Set;


/**
 * ZSet(SortedSet)-specific commands supported by Redis.
 * 
 * @author Costin Leau
 */
public interface RedisZSetCommands {

	public enum Aggregate {
		SUM, MIN, MAX;
	}

	public interface Tuple {
		String getValue();

		Double getScore();
	}

	Boolean zAdd(String key, double score, String value);

	Boolean zRem(String key, String value);

	Double zIncrBy(String key, double increment, String value);

	Integer zRank(String key, String value);

	Integer zRevRank(String key, String value);

	Set<String> zRange(String key, int start, int end);

	Set<Tuple> zRangeWithScore(String key, int start, int end);

	Set<String> zRevRange(String key, int start, int end);

	Set<Tuple> zRevRangeWithScore(String key, int start, int end);

	Set<String> zRangeByScore(String key, double min, double max);

	Set<Tuple> zRangeByScoreWithScore(String key, double min, double max);

	Set<String> zRangeByScore(String key, double min, double max, int offset, int count);

	Set<Tuple> zRangeByScoreWithScore(String key, double min, double max, int offset, int count);

	Integer zCount(String key, double min, double max);

	Integer zCard(String key);

	Double zScore(String key, String value);

	Integer zRemRange(String key, int start, int end);

	Integer zRemRangeByScore(String key, double min, double max);

	Integer zUnionStore(String destKey, String... sets);

	Integer zUnionStore(String destKey, Aggregate aggregate, int[] weights, String... sets);

	Integer zInterStore(String destKey, String... sets);

	Integer zInterStore(String destKey, Aggregate aggregate, int[] weights, String... sets);
}