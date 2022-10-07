package mf;


import java.util.function.Supplier;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author Mark
 * @param <T>
 * @param <E>
 */
public class TryCatch<T, E extends Exception> {
    
    static public <T, E extends Exception> TryCatch<T, E> Try(ThrowingSupplier<T, E> tryThis) {
        return new TryCatch<>(tryThis);
    }
    
    private TryCatch(ThrowingSupplier<T, E> tryThis) {
        this.TRY_THIS = tryThis;
    }
    
    public T Catch(Class<E> exClass, Supplier<T> orThis) {
        try {
            return TRY_THIS.get();
        } catch (RuntimeException ex) {
            if (exClass.isInstance(ex)) return orThis.get();
            else throw ex;
        } catch(Exception ex) {
            return orThis.get();
        }            
    }
    
    public T Catch(Supplier<T> orThis) {
        try {
            return TRY_THIS.get();
        } catch(Exception ex) {
            return orThis.get();
        }            
    }
    
    final private ThrowingSupplier<T, E> TRY_THIS;


    @FunctionalInterface
    static public interface ThrowingSupplier<T, E extends Exception> {
        T get() throws E;
    }
    
    @SuppressWarnings("unused")
	static private final void DoSomeTests() {
        String result1 = Try(() -> {
            return "TEST";
        }).Catch(() -> "FAIL");
        assert "TEST".equals(result1);
        
        int result2 = Try(() -> {
            return Integer.parseInt("not a valid integer");
        }).Catch(() -> 10);
        assert result2 == 10;
        
        String result3 = Try(() -> {
            if (1==2) throw new java.io.IOException("Test - throwing an IOException");
            else return "FAIL";
        }).Catch(java.io.IOException.class, () -> "TEST");
        assert "TEST".equals(result3);
        
    }
    
 
	static void main(String[] args) {
		DoSomeTests();
	}
}
