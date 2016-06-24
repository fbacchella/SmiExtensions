# SmiExtensions
Helper classes to parse a MIB database that can be used with SNMP4J

It provides a way to resolve OID using string instead of numerical notation. It don't try to parse mib. Instead it expect
that to be done by net-snmp and used a tree dump.

To get a full dump if net-snmp is fully configured, one can use the command:

    snmptranslate -Tp
    
Dump for local dump are generated using:

    snmptranslate -Tp -m ALL -M .../path_to_mibs_files

And then to use it in SNMP4J:

    import org.snmp4j.SNMP4JSettings;
    import fr.jrds.SmiExtensions.MibTree;
    import java.io.FileReader
  
    public void init() {
        MibTree resolver = new MibTree();
        // And then for each custom dump
        resolver.load(new FileReader(".../path_to_additionnals_tree_dump"));
        SNMP4JSettings.setOIDTextFormat(new OIDFormatter(resolver));
    }
