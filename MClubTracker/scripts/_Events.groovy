import groovy.xml.StreamingMarkupBuilder


eventWebXmlEnd = {String tmpfile ->
	
    def root = new XmlSlurper().parse(webXmlFile)
    root.appendNode {
       'listener' {
		   'listener-class' (
			   'mclub.tracker.LivePositionWebsocketServer'
		   )
        }
	   'listener' {
		   'listener-class' (
			   'mclub.tracker.TalkBoxWebsocketServer'
		   )
		}
    }
    webXmlFile.text = new StreamingMarkupBuilder().bind {
        mkp.declareNamespace(
                "": "http://java.sun.com/xml/ns/javaee")
        mkp.yield(root)
    }
}