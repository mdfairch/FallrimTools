/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mf;

/**
 *
 * @author Mark
 */
final public class ConsoleProgress {

    public ConsoleProgress() {
        reset(null, 1);
    }
    
    public <T> void reset(String newLabel, java.util.Collection<T> collection) {
        reset(newLabel, collection.size());
    }
    
    public <S,T> void reset(String newLabel, java.util.Map<S,T> map) {
        reset(newLabel, map.size());
    }
    
    public <T> void reset(String newLabel, T[] arr) {
        reset(newLabel, arr.length);
    }
    
    public void reset(String newLabel, int newTotal) {
        assert newTotal >= 0;
        current = 0;
        total = newTotal;
        division = newTotal / 70;
        label = newLabel;        
        timer = Timer.startNew(newLabel);
    }
    
    public <T> boolean inc(T v) {
        inc();
        return true;
    }
    
    public void inc() {
        if (current == 0 && label != null) {
            System.out.print(label);            
        }
        
        current += 1;
        
        if (current == total) {
            System.out.println(timer.getFormattedTime());            
        }
        else if (division > 1 && current % division == 0) {
            System.out.print('.');
            System.out.flush();
        }
    }
    
    private Timer timer = null;
    private int current = 0;
    private String label;
    private int total;
    private int division;
}
