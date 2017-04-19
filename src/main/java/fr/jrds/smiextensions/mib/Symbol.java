package fr.jrds.smiextensions.mib;

public class Symbol {
    public final String module;
    public final String name;
    Symbol(String module, String name) {
        this.module = module;
        this.name = name;
    }
    @Override
    public String toString() {
        return module + "::" + name;
    }
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((module == null) ? 0 : module.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }
    @Override
    public boolean equals(Object obj) {
        if(this == obj)
            return true;
        if(obj == null)
            return false;
        if(getClass() != obj.getClass())
            return false;
        Symbol other = (Symbol) obj;
        if(module == null) {
            if(other.module != null)
                return false;
        } else if(!module.equals(other.module))
            return false;
        if(name == null) {
            if(other.name != null)
                return false;
        } else if(!name.equals(other.name))
            return false;
        return true;
    }
}
