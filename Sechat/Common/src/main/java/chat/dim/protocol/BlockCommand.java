/* license: https://mit-license.org
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Albert Moky
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * ==============================================================================
 */
package chat.dim.protocol;

import java.util.List;
import java.util.Map;

import chat.dim.dkd.BaseCommand;

/**
 *  Command message: {
 *      type : 0x88,
 *      sn   : 123,
 *
 *      cmd  : "block",
 *      list : []      // block-list
 *  }
 */
public class BlockCommand extends BaseCommand {

    public static final String BLOCK   = "block";

    // Block-list
    private List<ID> blockList = null;

    public BlockCommand(Map<String, Object> dictionary) {
        super(dictionary);
    }

    /**
     *  Send block-list
     *
     * @param list - block list
     */
    public BlockCommand(List<ID> list) {
        super(BLOCK);
        setBlockCList(list);
    }

    /**
     *  Query block-list
     */
    public BlockCommand() {
        super(BLOCK);
    }

    //-------- setters/getters --------

    @SuppressWarnings("unchecked")
    public List<ID> getBlockCList() {
        if (blockList == null) {
            Object list = get("list");
            if (list != null) {
                blockList = ID.convert((List<String>) list);
            }
        }
        return blockList;
    }

    public void setBlockCList(List<ID> list) {
        if (list == null) {
            remove("list");
        } else {
            put("list", ID.revert(list));
        }
        blockList = list;
    }
}
