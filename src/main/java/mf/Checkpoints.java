/*
 * Copyright 2023 Mark.
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
package mf;

import java.text.MessageFormat;
import java.util.Objects;

/**
 *
 * @author Mark Fairchild
 */
public class Checkpoints {
    
    public Checkpoints() {
    }
    
    public void AddCheckPoint(String name, long point) {
        this.POINTS.addLast(Pair.of(name, point));
    }
    
    public void VerifyCheckPoint(String name, long point) {
        if (this.POINTS.isEmpty()) {
            throw new IllegalStateException(String.format("Unmatched checkpoint (%s : %d)", name, point));
        }
        
        Pair<String, Long> checkpoint = this.POINTS.removeFirst();
        
        if (point != checkpoint.B) 
        {
            throw new IllegalStateException(String.format("Checkpoint doesn't match (%s : %d) != (%s : %d)", checkpoint.A, checkpoint.B, name, point));
        }
    }
    
    final private java.util.LinkedList<Pair<String, Long>> POINTS = new java.util.LinkedList();
    //final private MessageFormat MSG1 = new MessageFormat("Unmatched checkpoint ({0} : {1,number})");
    //final private MessageFormat MSG2 = new MessageFormat("Checkpoint doesn't match ({0} : {1,number}) != ({2} : {3,number})");
    
}
