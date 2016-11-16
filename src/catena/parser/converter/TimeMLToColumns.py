import re
import os
import time
import subprocess
import xml.etree.ElementTree as ET

import sys;
reload(sys);
sys.setdefaultencoding("utf8")

class TimeMLToColumns:
    def __init__(self, timeml_filename, parser="textpro"):
        self.filename = timeml_filename
        self.timeml = open(timeml_filename, "r").read()
        self.content = ""
        self.dct = ""
        self.sentences = []
        self.entities = []
        self.entity_types = {}
        self.eventids = {}
        self.instances = {}
        self.tlinks = {}
        self.slinks = {}
        self.alinks = {}
        self.clinks = {}

        self.num_tlink = 0

        self.stanford_corenlp = False
        self.textpro = False        
        if parser == "stanford": self.stanford_corenlp = True
        elif parser == "textpro": self.textpro = True

    #parse EVENT and TIMEX3 and SIGNAL
    def __parseTimex(self, text):   #for DCT
        (timex_tag, timex_text) = re.findall(r'<(TIMEX3.*?)>(.+?)</TIMEX3>', text)[0]
        (tid, ttype, tvalue, tanchor, tfunc, tfuncdoc) = self.__parseTimexTag(timex_tag)
        return (timex_text, (tid, ttype, tvalue, tanchor, tfunc, tfuncdoc))
    
    def __parseEventTag(self, text):
        eid = re.findall(r'eid=\"(.+?)\"', text)[0]
        if len(re.findall(r'class=\"(.+?)\"', text)) != 0: eclass = re.findall(r'class=\"(.+?)\"', text)[0]
        else: eclass = "O"
        if len(re.findall(r'stem=\"(.+?)\"', text)) != 0: estem = re.findall(r'stem=\"(.+?)\"', text)[0]
        else: estem = "O"
        return (eid, eclass, estem)    

    def __parseTimexTag(self, text):
        tid = re.findall(r'tid=\"(.+?)\"', text)[0]
        ttype = re.findall(r'type=\"(.+?)\"', text)[0]
        tvalue = re.findall(r'value=\"(.+?)\"', text)[0]
        if len(re.findall(r'anchorTimeID=\"(.+?)\"', text)) != 0:
            tanchor = re.findall(r'anchorTimeID=\"(.+?)\"', text)[0]
        else:
            tanchor = "O"
        if len(re.findall(r'temporalFunction=\"(.+?)\"', text)) != 0:
            tfunc = re.findall(r'temporalFunction=\"(.+?)\"', text)[0]
        else:
            tfunc = "O"
        if len(re.findall(r'functionInDocument=\"(.+?)\"', text)) != 0:
            tfuncdoc = re.findall(r'functionInDocument=\"(.+?)\"', text)[0]
        else:
            tfuncdoc = "O"
        return (tid, ttype, tvalue, tanchor, tfunc, tfuncdoc)
        
    def __parseSignalTag(self, text):
        sid = re.findall(r'sid=\"(.+?)\"', text)[0]
        return sid

    def __parseCSignalTag(self, text):
        cid = re.findall(r'cid=\"(.+?)\"', text)[0]
        return cid

    #parse instances and event attributes
    def __parseEventInstances(self):
        tree = ET.parse(self.filename)
        root = tree.getroot()
        for instance in root.findall("MAKEINSTANCE"):
            self.eventids[instance.get("eiid")] = instance.get("eventID")
            if instance.get("modality") == None or instance.get("modality") == "" or instance.get("modality") == "None": modality = "NONE"
            else: modality = instance.get("modality")
            self.instances[instance.get("eventID")] = (instance.get("tense"), instance.get("aspect"), instance.get("polarity"), modality, instance.get("pos"))

    #parse TLINKs
    def __parseTLINKs(self):
        tree = ET.parse(self.filename)
        root = tree.getroot()
        for tlink in root.findall("TLINK"):
            relType = tlink.get("relType")

            entID = ""
            if tlink.get("eventInstanceID") is not None:
                entID = self.eventids[tlink.get("eventInstanceID")]
            else:
                entID = tlink.get("timeID")

            relentID = ""
            if tlink.get("relatedToEventInstance") is not None:
                relentID = self.eventids[tlink.get("relatedToEventInstance")]
            else:
                relentID = tlink.get("relatedToTime")

            if tlink.get("signalID") is not None:
                signalID = tlink.get("signalID")
            else:
                signalID = "O"

            if entID not in self.tlinks:
                self.tlinks[entID] = []
            self.tlinks[entID].append((relentID, relType, signalID))

    #parse SLINKs
    def __parseSLINKs(self):
        tree = ET.parse(self.filename)
        root = tree.getroot()
        for slink in root.findall("SLINK"):
            relType = slink.get("relType")
            entID = self.eventids[slink.get("eventInstanceID")]
            subentID = self.eventids[slink.get("subordinatedEventInstance")]
            if slink.get("signalID") is not None:
                signalID = slink.get("signalID")
            else:
                signalID = "O"

            if entID not in self.slinks:
                self.slinks[entID] = []
            self.slinks[entID].append((subentID, relType, signalID))

    #parse ALINKs
    def __parseALINKs(self):
        tree = ET.parse(self.filename)
        root = tree.getroot()
        for alink in root.findall("ALINK"):
            relType = alink.get("relType")
            entID = self.eventids[alink.get("eventInstanceID")]
            relentID = self.eventids[alink.get("relatedToEventInstance")]
            if alink.get("signalID") is not None:
                signalID = alink.get("signalID")
            else:
                signalID = "O"

            if entID not in self.alinks:
                self.alinks[entID] = []
            self.alinks[entID].append((relentID, relType, signalID))

    #parse CLINKs
    def __parseCLINKs(self):
        tree = ET.parse(self.filename)
        root = tree.getroot()
        for clink in root.findall("CLINK"):
            entID = self.eventids[clink.get("eventInstanceID")]
            relentID = self.eventids[clink.get("relatedToEventInstance")]
            if clink.get("c-signalID") is not None and clink.get("c-signalID") != "cnull":
                csignalID = clink.get("c-signalID")
            else:
                csignalID = "O"

            if entID not in self.clinks:
                self.clinks[entID] = []
            self.clinks[entID].append((relentID, csignalID))

    #parse tokenization output from Stanford CoreNLP
    def __parseStanfordOutput(self, xml_filepath):
        tree = ET.parse(xml_filepath)
        doc = tree.getroot()
        sentences = doc.find('document').find('sentences').findall('sentence')
        for sen in sentences:
            words = []
            event_attr = None
            timex_attr = None
            signal_id = None
            csignal_id = None

            sid = int(sen.get('id'))
            tokens = sen.find('tokens').findall('token')
            for tok in tokens:
                word = tok.find('word').text
                word = word.replace(u'\xa0', " ")
                #print word
                if "<EVENT" in word:
                    event_attr = self.__parseEventTag(word)
                    self.entities.append(event_attr[0])
                    self.entity_types[event_attr[0]] = "e"
                elif "</EVENT>" in word:
                    event_attr = None
                elif "<TIMEX3" in word:
                    timex_attr = self.__parseTimexTag(word)
                    self.entities.append(timex_attr[0])
                    self.entity_types[timex_attr[0]] = "tmx"
                elif "</TIMEX3>" in word:
                    timex_attr = None
                elif "<SIGNAL" in word:
                    signal_id = self.__parseSignalTag(word)
                    self.entities.append(signal_id)
                    self.entity_types[signal_id] = "s"
                elif "</SIGNAL>" in word:
                    signal_id = None
                elif "<CSIGNAL" in word:
                    csignal_id = self.__parseCSignalTag(word)
                    self.entities.append(csignal_id)
                    self.entity_types[csignal_id] = "cs"
                elif "</CSIGNAL>" in word:
                    csignal_id = None
                else: 
                    words.append((word.replace(u'\xa0', " "), event_attr, timex_attr, signal_id, csignal_id))
            self.sentences.append(words)

    #parse tokenization output from TextPro
    def __parseTextProOutput(self, txp_filepath):
        txpfile = open(txp_filepath, 'r')
       
        #avoid the file description
        for i in range(4): txpfile.readline()

        words = []
        event_tag = ""
        timex_tag = ""
        signal_tag = ""
        csignal_tag = ""
        event_attr = None
        timex_attr = None
        signal_id = None
        csignal_id = None
        
        while True:
            line = txpfile.readline()
            if not line: break    

            #print line.strip()
        
            #process line
            if line.strip() == "<": 
                continue
            elif line.strip() == "EVENT":
                event_tag += line.strip()
                while True:
                    line2 = txpfile.readline()
                    if line2.strip() == ">":
                        break
                    else:
                        event_tag += line2.strip()
                event_attr = self.__parseEventTag(event_tag)
                self.entities.append(event_attr[0])
                self.entity_types[event_attr[0]] = "e"
            elif line.strip() == "</EVENT>":    
                event_tag = ""
                event_attr = None
            elif line.strip() == "TIMEX3":
                timex_tag += line.strip()
                while True:
                    line2 = txpfile.readline()
                    if line2.strip() == ">":
                        break
                    else:
                        timex_tag += line2.strip()
                timex_attr = self.__parseTimexTag(timex_tag)
                self.entities.append(timex_attr[0])
                self.entity_types[timex_attr[0]] = "tmx"
            elif line.strip() == "</TIMEX3>":    
                timex_tag = ""
                timex_attr = None
            elif "</TIMEX3>" in line.strip():    #there is "May</TIMEX3>." -.-
                tokks = line.strip().split("</TIMEX3>")
                words.append((tokks[0], event_attr, timex_attr, signal_id, csignal_id))
                timex_tag = ""
                timex_attr = None
                words.append((tokks[1], event_attr, timex_attr, signal_id, csignal_id))
                if tokks[1] == ".": 
                    self.sentences.append(words)
                    words = []
            elif line.strip() == "SIGNAL":
                signal_tag += line.strip()
                while True:
                    line2 = txpfile.readline()
                    if line2.strip() == ">":
                        break
                    else:
                        signal_tag += line2.strip()
                signal_id = self.__parseSignalTag(signal_tag)
                self.entities.append(signal_id)
                self.entity_types[signal_id] = "s"
            elif line.strip() == "</SIGNAL>":    
                signal_tag = ""
                signal_id = None
            elif line.strip() == "CSIGNAL":
                csignal_tag += line.strip()
                while True:
                    line2 = txpfile.readline()
                    if line2.strip() == ">":
                        break
                    else:
                        csignal_tag += line2.strip()
                csignal_id = self.__parseCSignalTag(csignal_tag)
                self.entities.append(csignal_id)
                self.entity_types[csignal_id] = "cs"
            elif line.strip() == "</CSIGNAL>":    
                csignal_tag = ""
                csignal_id = None
            elif line.strip() == "" and (timex_tag == "" and event_tag == "" and signal_tag == "" and csignal_tag == ""):
                self.sentences.append(words)
                words = []
            elif line.strip() == "" and (timex_tag != "" or event_tag != "" or signal_tag != "" or csignal_tag != ""):
                continue
            else:
                words.append((line.strip(), event_attr, timex_attr, signal_id, csignal_id))

        #print self.sentences

    def __getEntityID(self, eid):
        if eid == "t0": return "tmx" + str(0)
        elif eid == "O": return eid
        else: return self.entity_types[eid] + str(self.entities.index(eid) + 1)

    def __getLinkString(self, links, entity_id):
        link_str = ""
        if entity_id in links:
            for link in links[entity_id]:
                #           entity_id                             related_entity_id                   rel_type        signal_id
                link_str += self.__getEntityID(entity_id) + ":" + self.__getEntityID(link[0]) + ":" + link[1] + ":" + self.__getEntityID(link[2]) + "||"
            link_str = link_str[0:-2]
        else:
            link_str = "O"
        return link_str

    def __getCLinkString(self, links, entity_id):
        link_str = ""
        if entity_id in links:
            for link in links[entity_id]:
                #           entity_id                             related_entity_id                   signal_id
                link_str += self.__getEntityID(entity_id) + ":" + self.__getEntityID(link[0]) + ":" + self.__getEntityID(link[1]) + "||"
            link_str = link_str[0:-2]
        else:
            link_str = "O"
        return link_str

    def __buildColumns(self):
        line = ""

        #0     1        2       3        4     5    6     7      8   9   10  11     12       13      14         15       16          17     18     19     20     21        22
        #token token_id sent_id ev_id ev_class stem tense aspect pol mod pos tmx_id tmx_type tmx_val tmx_anchor tmx_func tmx_funcdoc TLINKs SLINKs ALINKs CLINKs signal_id c-signal_id
        
        #DCT
        (dct_text, (dct_id, dct_type, dct_value, dct_anchor, dct_func, dct_funcdoc)) = self.dct
        line += "DCT\t-99\t-99"
        for i in range(8): line += "\tO"    #event  

        #timex    
        line += "\t" + self.__getEntityID(dct_id) + "\tB-" + dct_type + "\t" + dct_value + "\tO\tO\t" + dct_func

        line += "\t" + self.__getLinkString(self.tlinks, dct_id)  #tlinks
        line += "\tO"   #slinks
        line += "\tO"   #alinks
        line += "\tO"   #clinks
        line += "\tO"   #signal
        line += "\tO"   #csignal
        line += "\n\n"

        sent_id = 0
        tok_id = 1
        prev_timex_id = None
        prev_event_id = None
        (tlink_str, slink_str, alink_str, clink_str) = ("O", "O", "O", "O")

        for eid in self.tlinks:
            self.num_tlink += len(self.tlinks[eid])

        #TEXT
        for sen in self.sentences:
            if len(sen) > 0:
                for (word, event_attr, timex_attr, signal_id, csignal_id) in sen:
                    line += word + "\t" + str(tok_id) + "\t" + str(sent_id)

                    (tlink_str, slink_str, alink_str, clink_str) = ("O", "O", "O", "O")
                    
                    #event attributes if any: (event_id, event_class, stem, tense, aspect, polarity, modality, pos)
                    if event_attr is not None:
                        line += "\t" + self.__getEntityID(event_attr[0])    #event_id

                        #event_class, stem
                        for i in range(1, len(event_attr)):
                            if i==1:
                                if prev_event_id != event_attr[0]: 
                                    line += "\tB-" + event_attr[i]
                                    prev_event_id = event_attr[0]
                                else: line += "\tI-" + event_attr[i]
                            else: line += "\t" + event_attr[i]

                        #tense, aspect, polarity, modality, pos
                        eid = event_attr[0]
                        if eid in self.instances:
                            for eatr in self.instances[eid]:
                                if eatr is None or eatr == "":
                                    line += "\tO"
                                else:
                                    line += "\t" + eatr.replace(" ", "_")
                        else:
                            for i in range(5): line += "\tO"

                        tlink_str = self.__getLinkString(self.tlinks, eid)  #tlinks
                        slink_str = self.__getLinkString(self.slinks, eid)  #slinks
                        alink_str = self.__getLinkString(self.alinks, eid)  #alinks
                        clink_str = self.__getCLinkString(self.clinks, eid) #clinks

                    else:
                        prev_event_id = None
                        for i in range(8): line += "\tO"

                    #timex attributes if any: (timex_id, timex_type, timex_value, anchor, tfunc, tfuncdoc)
                    if timex_attr is not None:
                        line += "\t" + self.__getEntityID(timex_attr[0])    #timex_id

                        #timex_type, timex_value, anchor, tfunc, tfuncdoc
                        for i in range(1, len(timex_attr)):
                            if i==1: 
                                if prev_timex_id != timex_attr[0]: 
                                    line += "\tB-" + timex_attr[i]
                                    prev_timex_id = timex_attr[0]
                                else: line += "\tI-" + timex_attr[i]
                            elif i==3: 
                                line += "\t" + self.__getEntityID(timex_attr[i])
                            else: line += "\t" + timex_attr[i]

                        tid = timex_attr[0]
                        tlink_str = self.__getLinkString(self.tlinks, tid)  #tlinks

                    else:
                        prev_timex_id = None
                        for i in range(6): line += "\tO"

                    line += "\t" + tlink_str
                    line += "\t" + slink_str
                    line += "\t" + alink_str
                    line += "\t" + clink_str

                    #signal attributes if any: signal_id
                    if signal_id is not None: line += "\t" + str(self.entities.index(signal_id)+1)
                    else: line += "\tO"

                    #causal signal attributes if any: csignal_id
                    if csignal_id is not None: line += "\t" + str(self.entities.index(csignal_id)+1)
                    else: line += "\tO"

                    line += "\n"
                    tok_id += 1
                line += "\n"
                sent_id += 1

        return line.strip()
        
    def parseTimeML(self):
        dct_text = re.findall(r'<%s>(.+?)<%s>' % ('DCT','/DCT'), self.timeml)[0]
        self.dct = self.__parseTimex(dct_text)
        self.content = re.findall(r'<%s>(.+?)<%s>' % ('TEXT','/TEXT'), self.timeml, re.DOTALL)[0]

        self.content = self.content.replace("``", "\"")
        self.content = self.content.replace("''", "\"")
        self.content = self.content.replace("C-SIGNAL", "CSIGNAL")
        
        temp = open("temp", 'w')
        temp.write(self.content.strip())
        temp.close()

        self.__parseEventInstances()
        self.__parseTLINKs()
        self.__parseSLINKs()
        self.__parseALINKs()
        self.__parseCLINKs()

        config = {}
        config_file = open("converter/parser.config", "r")
        for line in config_file:
            x = line.split("=")
            config[x[0].strip()] = x[1].strip()[1:-1]

        if self.stanford_corenlp:
            with open("log", 'w') as logfile:
                command = "java -cp " + config["STANFORD_CORENLP_PATH"] + "stanford-corenlp-" + config["STANFORD_CORENLP_VERSION"] + ".jar:" + config["STANFORD_CORENLP_PATH"] + "stanford-corenlp-" + config["STANFORD_CORENLP_VERSION"] + "-models.jar:" + config["STANFORD_CORENLP_PATH"] + "xom.jar:"+ config["STANFORD_CORENLP_PATH"] + "joda-time.jar:" + config["STANFORD_CORENLP_PATH"] + "jollyday.jar -Xmx2g edu.stanford.nlp.pipeline.StanfordCoreNLP -annotators tokenize,ssplit -file temp -outputFormat xml"
                subprocess.call(command.split(" "), stderr=logfile)
            self.__parseStanfordOutput("temp.xml")
        elif self.textpro:
            command = "sh " + config["TEXTPRO_PATH"] + "textpro.sh -l eng -c token -y temp"
            #command = "perl ./TextPro1.5.2/textpro.pl -l eng -c token -y temp"
            subprocess.call(command.split(" "))
            self.__parseTextProOutput("temp.txp")

        os.remove("temp")
        if self.stanford_corenlp: 
            os.remove("temp.xml")
            os.remove("log")
        elif self.textpro:
            os.remove("temp.txp")

        #print self.__buildColumns()
        return self.__buildColumns()

    def getNumTLINK(self):
        return self.num_tlink

#timeml_cols = TimeMLToColumns("ABC19980108.1830.0711.tml")
#timeml_cols.parseTimeML()
