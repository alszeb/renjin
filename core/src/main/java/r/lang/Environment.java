/*
 * R : A Computer Language for Statistical Data Analysis
 * Copyright (C) 1995, 1996  Robert Gentleman and Ross Ihaka
 * Copyright (C) 1997-2008  The R Development Core Team
 * Copyright (C) 2003, 2004  The R Foundation
 * Copyright (C) 2010 bedatadriven
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package r.lang;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.UnmodifiableIterator;
import r.lang.primitive.BaseFrame;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

/**
 * The Environment data type.
 *
 * <p>
 * Environments can be thought of as consisting of two things:
 * <ul>
 * <li>A <strong>frame</strong>, consisting of a set of symbol-value pairs, and
 * <li>an enclosure, a pointer to an enclosing environment.</li>
 * </ul>
 *
 * <p>
 * When R looks up the vbalue for a symbol the frame is examined and if a
 * matching symbol is found its value will be returned. If not, the enclosing environment
 *  is then accessed and the process repeated.
 * Environments form a tree structure in which the enclosures play the role of parents.
 *  The tree of environments is rooted in an empty environment,
 * available through emptyenv(), which has no parent.
 * It is the direct parent of the environment of the base package
 * (available through the baseenv() function). Formerly baseenv() 
 * had the special value {@code NULL}, but as from version 2.4.0, the
 *  use of {@code NULL} as an environment is defunct.
 *
 */
public class Environment extends AbstractSEXP implements Recursive {

  public static final int TYPE_CODE = 4;
  public static final String TYPE_NAME = "environment";

  private String name;
  private Environment parent;
  private Environment globalEnvironment;
  private Environment baseEnvironment;


  /**
   * The root of the environment hierarchy.
   */
  public static final EmptyEnv EMPTY = new EmptyEnv();

  public static Environment createGlobalEnvironment() {
    Environment global = new Environment();
    global.name = "R_GlobalEnv";
    global.baseEnvironment = createBaseEnvironment(global);
    global.globalEnvironment = global;
    global.parent = global.baseEnvironment;
    global.frame = new HashFrame();

    return global;
  }

  private static Environment createBaseEnvironment(Environment global) {
    Environment base = new Environment();
    base.name = "base";
    base.baseEnvironment = base;
    base.globalEnvironment = global;
    base.parent = EMPTY;
    base.frame = BaseFrame.INSTANCE;
    return base;
  }

  public static Environment createChildEnvironment(Environment parent) {
    Environment child = new Environment();
    child.name = Integer.toString(child.hashCode());
    child.baseEnvironment = parent.baseEnvironment;
    child.globalEnvironment = parent.globalEnvironment;
    child.parent = parent;
    child.frame = new HashFrame();
    return child;
  }

  public void setVariables(PairList pairList) {
    for(PairList.Node node : pairList.nodes()) {
      if(!node.hasTag()) {
        throw new IllegalArgumentException("All elements of pairList must be tagged");
      }
      setVariable(node.getTag(), node.getValue());
    }
  }

  public interface Frame {
    Set<Symbol> getSymbols();
    SEXP getVariable(Symbol name);
    SEXP getInternal(Symbol name);
    void setVariable(Symbol name, SEXP value);
  }

  public static class HashFrame implements Frame{
    private HashMap<Symbol, SEXP> values = new HashMap<Symbol, SEXP>();

    @Override
    public Set<Symbol> getSymbols() {
      return values.keySet();
    }

    @Override
    public SEXP getVariable(Symbol name) {
      SEXP value = values.get(name);
      return value == null ? Symbol.UNBOUND_VALUE : value;
    }

    @Override
    public SEXP getInternal(Symbol name) {
      return Symbol.UNBOUND_VALUE;
    }

    @Override
    public void setVariable(Symbol name, SEXP value) {
      values.put(name, value);
    }
  }

  protected Frame frame;

  public String getName() {
    return name;
  }

  public Environment getParent() {
    return parent;
  }

  public void setParent(Environment parent) {
    this.parent = parent;
  }

  public Environment getGlobalEnvironment() {
    return globalEnvironment;
  }

  public Environment getBaseEnvironment() {
    return baseEnvironment;
  }

  @Override
  public int getTypeCode() {
    return TYPE_CODE;
  }

  @Override
  public String getTypeName() {
    return TYPE_NAME;
  }

  public Collection<Symbol> getSymbolNames() {
    return frame.getSymbols();
  }

  public void setVariable(Symbol symbol, SEXP value) {
    frame.setVariable(symbol, value);
  }

  /**
   * Searches the environment for a value that matches the given predicate.
   *
   * @param symbol The symbol for which to search
   * @param predicate a predicate that tests possible return values
   * @param inherits if {@code true}, enclosing frames are searched
   * @return
   */
  public SEXP findVariable(Symbol symbol, Predicate<SEXP> predicate, boolean inherits) {
    SEXP value = frame.getVariable(symbol);
    if(value != Symbol.UNBOUND_VALUE && predicate.apply(value)) {
      return value;
    }
    return parent.findVariable(symbol, predicate, inherits);
  }

  public final SEXP findVariable(Symbol symbol) {
    return findVariable(symbol, Predicates.<SEXP>alwaysTrue(), true);
  }

  public SEXP findInternal(Symbol symbol) {
    SEXP value = frame.getInternal(symbol);
    if(value != Symbol.UNBOUND_VALUE) {
      return value;
    }
    return parent.findInternal(symbol);
  }

  @Override
  public void accept(SexpVisitor visitor) {
    visitor.visit(this);
  }

  public Iterable<Environment> selfAndParents() {
    return new Iterable<Environment>() {
      @Override
      public Iterator<Environment> iterator() {
        return new EnvIterator(Environment.this);
      }
    };
  }

  public SEXP getVariable(Symbol symbol) {
    return frame.getVariable(symbol);
  }

  public SEXP getVariable(String symbolName) {
    return getVariable(new Symbol(symbolName));
  }

  public boolean hasVariable(Symbol symbol) {
    return frame.getVariable(symbol) != Symbol.UNBOUND_VALUE;
  }
  
  private static class EnvIterator extends UnmodifiableIterator<Environment> {
    private Environment next;

    private EnvIterator(Environment next) {
      this.next = next;
    }

    @Override
    public boolean hasNext() {
      return next != EMPTY;
    }

    @Override
    public Environment next() {
      Environment toReturn = next;
      next = next.parent;
      return toReturn;
    }
  }

  @Override
  public String toString() {
    return "<environment: " + name + ">";
  }

  private static class EmptyEnv extends Environment {

    private EmptyEnv() {
    }

    @Override
    public SEXP findVariable(Symbol symbol, Predicate<SEXP> predicate, boolean inherits) {
      return Symbol.UNBOUND_VALUE;
    }

    @Override
    public SEXP getVariable(Symbol symbol) {
      return Symbol.UNBOUND_VALUE;
    }

    @Override
    public SEXP findInternal(Symbol symbol) {
      return Symbol.UNBOUND_VALUE;
    }

    @Override
    public Environment getParent() {
      throw new UnsupportedOperationException("The empty environment does not have a parent.");
    }

    @Override
    public void setParent(Environment parent) {
      throw new UnsupportedOperationException("The empty environment does not have a parent.");
    }
  }
}