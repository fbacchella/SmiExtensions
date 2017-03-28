# SmiExtensions
Helper classes to parse a MIB database that can be used with SNMP4J

It provides a way to resolve OID using string instead of numerical notation. It don't try to parse MIB. Instead it expect
that to be done by net-snmp and used a tree dump.

To get a full dump if net-snmp is fully configured, one can use the command:

    snmptranslate -Tp
    
Dump for local dump are generated using:

    snmptranslate -Tp -m ALL -M .../path_to_mibs_files

And then to use it in SNMP4J:

        OIDFormatter.register()

The formatter can't handle all SNMP's textual convention. So it's up to the user to write custom one.
It's done by implementing the abstract class fr.jrds.SmiExtensions.objects.TextualConvention and then adding it in the tree:

    MibTree resolver = new MibTree();
    OIDFormatter formater = new OIDFormatter(resolver)
    formater.addTextualConvention(CustomConvention.class)

It can also be used to split an index as Java object

    MibTree resolver = new MibTree();
    Object[] parts = parseIndexOID(new OID("1.3.6.1.6.3.16.1.4.1.4.7.118.51.103.114.111.117.112.0.3.1"))
    Arrays.stream(parts).forEach( i-> System.out.println("'" + i + "' " + i.getClass()));

Will output

    'vacmAccessContextMatch' class org.snmp4j.smi.OctetString
    'v3group' class java.lang.String
    '' class java.lang.String
    '3' class java.lang.Integer
    'noAuthNoPriv(1)' class java.lang.String
