import re
import os
import subprocess
import xml.etree.ElementTree as ET

import sys;
reload(sys);
sys.setdefaultencoding("utf8")

class ColumnsToCAT:
    def __init__(self, str_columns, filename):
        self.filename = filename
        self.str_columns = str_columns
        self.entities = []
        self.rel_id = 1

    def __createEvent(self, node, start_idx, end_idx, eid, eclass, stem, tense, aspect, polarity, modality, pos):
        node.set("id", eid)
        if eclass != "O": node.set("class", eclass[2:])
        if tense != "O": node.set("tense", tense)
        if aspect != "O": node.set("aspect", aspect)
        if polarity != "O": node.set("polarity", polarity)
        if modality != "O": node.set("modality", modality.replace("_", " "))
        if pos != "O": node.set("pos", pos)
        node.set("comment", "")
        node.set("factuality", "")
        node.set("certainty", "")
        if stem != "O": node.set("pred", stem)
        #else: node.set("pred", "")

        for i in range(start_idx, end_idx+1):
            anchor = ET.SubElement(node, "token_anchor")
            anchor.set("id", str(i))

    def __createTimex(self, node, start_idx, end_idx, tid, ttype, tvalue, tanchor, tfunction, tfunc_in_doc):
        node.set("id", tid)
        if tfunc_in_doc != "O": node.set("functionInDocument", tfunc_in_doc)
        if tfunc_in_doc == "CREATION_TIME": node.set("type", ttype)
        else: node.set("type", ttype[2:])
        node.set("value", tvalue)
        if tanchor != "O": node.set("anchorTimeID", tanchor)
        node.set("beginPoint", "")
        node.set("endPoint", "")
        node.set("comment", "")
        node.set("freq", "")
        node.set("quant", "") 

        if tfunc_in_doc == "CREATION_TIME":
            node.set("RELATED_TO", "")
            node.set("TAG_DESCRIPTOR", "Empty_Mark")
        else:
            for i in range(start_idx, end_idx+1):
                anchor = ET.SubElement(node, "token_anchor")
                anchor.set("id", str(i))

    def __createSignal(self, node, start_idx, end_idx, signal_id):
        node.set("id", signal_id)
        for i in range(start_idx, end_idx+1):
            anchor = ET.SubElement(node, "token_anchor")
            anchor.set("id", str(i))

    def __createLinks(self, node, rel_id, rel_type, signal_id, source_id, target_id, signal_name):
        node.set("id", str(rel_id))
        if rel_type != None: node.set("relType", rel_type)
        if signal_id != "O": node.set(signal_name, signal_id)
        node.set("comment", "")  

        source = ET.SubElement(node, "source")
        source.set("id", str(source_id))
        target = ET.SubElement(node, "target")
        target.set("id", str(target_id))  

    def __generateLinks(self, rel, link_col, rel_name):
        if link_col != "O":
            for link in link_col.split("||"):
                x = link.split(":")
                signal_name = "signalID"
                if rel_name == "CLINK":
                    (ent_id, relent_id, signal_id) = (x[0], x[1], x[2])
                    signal_name = "c-signalID"
                    rel_type = None
                else: (ent_id, relent_id, rel_type, signal_id) = (x[0], x[1], x[2], x[3])         
                link = ET.SubElement(rel, rel_name)
                self.__createLinks(link, self.rel_id, rel_type, signal_id, ent_id, relent_id, signal_name)
                self.rel_id += 1

    #generate CAT XML tree
    def __generateCATTree(self):
        #0     1        2       3        4     5    6     7      8   9   10  11     12       13      14         15       16          17     18     19     20     21        22
        #token token_id sent_id ev_id ev_class stem tense aspect pol mod pos tmx_id tmx_type tmx_val tmx_anchor tmx_func tmx_funcdoc TLINKs SLINKs ALINKs CLINKs signal_id c-signal_id
        
        doc_name = os.path.splitext(os.path.basename(self.filename))[0]
        doc = ET.Element("Document")
        doc.set("doc_name", doc_name)

        mark = ET.SubElement(doc, "Markables")
        rel = ET.SubElement(doc, "Relations")

        num = 0
        tid = 0
        #rel_id = 1

        (timex_id, start_tmx, end_tmx) = ("O", 0, 0)
        (event_id, start_ev, end_ev) = ("O", 0, 0)
        (ttype, tvalue, tanchor, tfunction, tfunc_in_doc) = ("", "", "", "", "")
        (eclass, stem, tense, aspect, polarity, modality, pos) = ("", "", "", "", "", "", "")

        (signal_id, start_sig, end_sig) = ("O", 0, 0)
        (csignal_id, start_csig, end_csig) = ("O", 0, 0)

        for line in self.str_columns.splitlines():
            rows = []
            if line != "":
                cols = line.split()
                #print len(cols), tid, sid, cols

                if cols[0][0:3] == "DCT":
                    (dct_id, dct_type, dct_value, dct_anchor, dct_function, dct_func_in_doc) = (cols[11], cols[12], cols[13], cols[14], cols[15], cols[16])
                    timex = ET.SubElement(mark, "TIMEX3")
                    self.__createTimex(timex, 0, 0, dct_id, dct_type, dct_value, dct_anchor, dct_function, dct_func_in_doc)
                    self.__generateLinks(rel, cols[17], "TLINK")

                else:
                    #token
                    tok = ET.SubElement(doc, "token")
                    tid = int(cols[1])
                    tok.set("id", str(cols[1]))
                    tok.set("sentence", cols[2])
                    tok.set("number", str(num))
                    tok.text = cols[0]

                    #print (event_id, start_ev, end_ev), (timex_id, start_tmx, end_tmx)

                    #EVENT
                    if cols[3] != "O":
                        if event_id == "O":
                            (event_id, start_ev, end_ev) = (cols[3], tid, tid)
                            (eclass, stem, tense, aspect, polarity, modality, pos) = (cols[4], cols[5], cols[6], cols[7], cols[8], cols[9], cols[10]) 
                            self.__generateLinks(rel, cols[15], "TLINK") 
                            self.__generateLinks(rel, cols[16], "SLINK")
                            self.__generateLinks(rel, cols[17], "ALINK")
                            self.__generateLinks(rel, cols[18], "CLINK")

                        elif cols[3] == event_id:
                            end_ev = tid
                        elif cols[3] != event_id and start_ev != 0 and end_ev != 0:
                            event = ET.SubElement(mark, "EVENT")
                            self.__createEvent(event, start_ev, end_ev, event_id, eclass, stem, tense, aspect, polarity, modality, pos)
                            (event_id, start_ev, end_ev) = (cols[3], tid, tid)
                            (eclass, stem, tense, aspect, polarity, modality, pos) = (cols[4], cols[5], cols[6], cols[7], cols[8], cols[9], cols[10]) 
                            self.__generateLinks(rel, cols[17], "TLINK") 
                            self.__generateLinks(rel, cols[18], "SLINK")
                            self.__generateLinks(rel, cols[19], "ALINK")
                            self.__generateLinks(rel, cols[20], "CLINK")
                    else:
                        if event_id != "O" and start_ev != 0 and end_ev != 0:
                            event = ET.SubElement(mark, "EVENT")
                            self.__createEvent(event, start_ev, end_ev, event_id, eclass, stem, tense, aspect, polarity, modality, pos)
                            (event_id, start_ev, end_ev) = ("O", 0, 0)
                            (eclass, stem, tense, aspect, polarity, modality, pos) = ("", "", "", "", "", "", "")

                    #TIMEX3
                    if cols[11] != "O":                    
                        if timex_id == "O":
                            (timex_id, start_tmx, end_tmx) = (cols[11], tid, tid)
                            (ttype, tvalue, tanchor, tfunction, tfunc_in_doc) = (cols[12], cols[13], cols[14], cols[15], cols[16])   
                            self.__generateLinks(rel, cols[17], "TLINK")
                        elif cols[11] == timex_id:
                            end_tmx = tid
                        elif cols[11] != timex_id and start_tmx != 0 and end_tmx != 0:
                            timex = ET.SubElement(mark, "TIMEX3")
                            self.__createTimex(timex, start_tmx, end_tmx, timex_id, ttype, tvalue, tanchor, tfunction, tfunc_in_doc)
                            (timex_id, start_tmx, end_tmx) = (cols[11], tid, tid)
                            (ttype, tvalue, tanchor, tfunction, tfunc_in_doc) = (cols[12], cols[13], cols[14], cols[15], cols[16])
                            self.__generateLinks(rel, cols[17], "TLINK")
                            
                    else:
                        if timex_id != "O" and start_tmx != 0 and end_tmx != 0:
                            timex = ET.SubElement(mark, "TIMEX3")
                            self.__createTimex(timex, start_tmx, end_tmx, timex_id, ttype, tvalue, tanchor, tfunction, tfunc_in_doc)
                            (timex_id, start_tmx, end_tmx) = ("O", 0, 0)
                            (ttype, tvalue, tanchor, tfunction, tfunc_in_doc) = ("", "", "", "", "")

                    #SIGNAL
                    if cols[21]!= "O":                    
                        if signal_id == "O":
                            (signal_id, start_sig, end_sig) = (cols[21], tid, tid)
                        elif cols[21] == signal_id:
                            end_sig = tid
                        elif cols[21] != signal_id and start_sig != 0 and end_sig != 0:
                            signal = ET.SubElement(mark, "SIGNAL")
                            self.__createSignal(signal, start_sig, end_sig, signal_id)
                            (signal_id, start_sig, end_sig) = (cols[21], tid, tid)
                            
                    else:
                        if signal_id != "O" and start_sig != 0 and end_sig != 0:
                            signal = ET.SubElement(mark, "SIGNAL")
                            self.__createSignal(signal, start_sig, end_sig, signal_id)
                            (signal_id, start_sig, end_sig) = ("O", 0, 0)

                    #C-SIGNAL
                    if cols[22]!= "O":                    
                        if csignal_id == "O":
                            (csignal_id, start_csig, end_csig) = (cols[22], tid, tid)
                        elif cols[22] == csignal_id:
                            end_csig = tid
                        elif cols[22] != csignal_id and start_csig != 0 and end_csig != 0:
                            csignal = ET.SubElement(mark, "C-SIGNAL")
                            self.__createSignal(csignal, start_csig, end_csig, csignal_id)
                            (csignal_id, start_csig, end_csig) = (cols[22], tid, tid)
                            
                    else:
                        if csignal_id != "O" and start_csig != 0 and end_csig != 0:
                            csignal = ET.SubElement(mark, "C-SIGNAL")
                            self.__createSignal(csignal, start_csig, end_csig, csignal_id)
                            (csignal_id, start_csig, end_csig) = ("O", 0, 0)                  
                
                    num += 1
            
        return doc

    '''
    copy and paste from http://effbot.org/zone/element-lib.htm#prettyprint
    it basically walks your tree and adds spaces and newlines so the tree is
    printed in a nice way
    '''
    def __indent(self, elem, level=0):
        i = "\n" + level*"  "
        if len(elem):
            if not elem.text or not elem.text.strip():
                elem.text = i + "  "
            if not elem.tail or not elem.tail.strip():
                elem.tail = i
            for elem in elem:
                self.__indent(elem, level+1)
            if not elem.tail or not elem.tail.strip():
                elem.tail = i
        else:
            if level and (not elem.tail or not elem.tail.strip()):
                elem.tail = i


    def __generateCATFile(self, filepath, cat_tree):
        #print CAT XML document
        self.__indent(cat_tree)
        outputfile = open(filepath, "w")
        #outputfile.write('<?xml version="1.0" ?>' + ET.tostring(cat_tree, encoding="us-ascii", method="xml"))
        tree = ET.ElementTree(cat_tree)
        tree.write(outputfile)
        outputfile.close()

    def __generateCATString(self, cat_tree):
        self.__indent(cat_tree)
        return ET.tostring(cat_tree, encoding="us-ascii", method="xml")
        
    def parseColumns(self):
        cat_tree = self.__generateCATTree()
        #self.__generateCATFile(self.filename.replace(".tml", ".xml"), cat_tree)
        return self.__generateCATString(cat_tree)

#parser = "textpro"
#timeml_cols = TimeMLToColumns.TimeMLToColumns("example.tml", parser)
#cols_cat = ColumnsToCAT(timeml_cols.parseTimeML(), timeml_cols.filename)
#print cols_cat.parseColumns()
