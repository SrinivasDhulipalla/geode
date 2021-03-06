/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.geode.redis.internal.executor.set;


import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.geode.redis.internal.ByteArrayWrapper;
import org.apache.geode.redis.internal.Coder;
import org.apache.geode.redis.internal.Command;
import org.apache.geode.redis.internal.ExecutionHandlerContext;
import org.apache.geode.redis.internal.RedisConstants;
import org.apache.geode.redis.internal.RedisResponse;
import org.apache.geode.redis.internal.executor.AbstractScanExecutor;

public class SScanExecutor extends AbstractScanExecutor {

  @Override
  public RedisResponse executeCommandWithResponse(Command command,
      ExecutionHandlerContext context) {
    List<byte[]> commandElems = command.getProcessedCommand();

    ByteArrayWrapper key = command.getKey();

    byte[] cAr = commandElems.get(2);
    String cursorString = Coder.bytesToString(cAr);
    int cursor = 0;
    Pattern matchPattern = null;
    String globMatchPattern = null;
    int count = DEFAULT_COUNT;

    try {
      cursor = Integer.parseInt(cursorString);
    } catch (NumberFormatException e) {
      return RedisResponse.error(ERROR_CURSOR);
    }

    if (cursor < 0) {
      return RedisResponse.error(ERROR_CURSOR);
    }

    if (commandElems.size() > 4) {
      try {
        byte[] bytes = commandElems.get(3);
        String tmp = Coder.bytesToString(bytes);
        if (tmp.equalsIgnoreCase("MATCH")) {
          bytes = commandElems.get(4);
          globMatchPattern = Coder.bytesToString(bytes);
        } else if (tmp.equalsIgnoreCase("COUNT")) {
          bytes = commandElems.get(4);
          count = Coder.bytesToInt(bytes);
        }
      } catch (NumberFormatException e) {
        return RedisResponse.error(ERROR_COUNT);
      }
    }

    if (commandElems.size() > 6) {
      try {
        byte[] bytes = commandElems.get(5);
        String tmp = Coder.bytesToString(bytes);
        if (tmp.equalsIgnoreCase("COUNT")) {
          bytes = commandElems.get(6);
          count = Coder.bytesToInt(bytes);
        }
      } catch (NumberFormatException e) {
        return RedisResponse.error(ERROR_COUNT);
      }
    }

    if (count < 0) {
      return RedisResponse.error(ERROR_COUNT);
    }

    try {
      matchPattern = convertGlobToRegex(globMatchPattern);
    } catch (PatternSyntaxException e) {
      return RedisResponse.error(RedisConstants.ERROR_ILLEGAL_GLOB);
    }

    RedisSetCommands redisSetCommands =
        new RedisSetCommandsFunctionExecutor(context.getRegionProvider().getDataRegion());
    List<Object> returnList = redisSetCommands.sscan(key, matchPattern, count, cursor);

    return RedisResponse.scan(returnList);
  }
}
