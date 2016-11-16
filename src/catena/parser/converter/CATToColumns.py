import re
import os
import time
import subprocess
import xml.etree.ElementTree as ET

import sys;
reload(sys);
sys.setdefaultencoding("utf8")

class CATToColumns:
    def __init__(self, cat_filename):
        self.filename = cat_filename
        self.cat = open(cat_filename, "r").read()
        self.dct = ""
        self.entities = {}
        self.dict_events = {}
        self.dict_timexes = {}
        self.dict_signals = {}
        self.dict_csignals = {}
        self.dict_tlinks = {}
        self.dict_slinks = {}
        self.dict_alinks = {}
        self.dict_clinks = {}
        self.arr_tokens = []

        self.num_tlink = 0

    def __getEntityID(self, eid):
        if eid == "" or eid is None: return "O"
        else: return self.entities[eid][0] + eid

    def __getLinkString(self, links, entity_id, source_id):
        link_str = ""
        if source_id in links:
            for link in links[source_id]:
                #           entity_id                             related_entity_id                   rel_type        signal_id
                link_str += self.__getEntityID(entity_id) + ":" + self.__getEntityID(link[0]) + ":" + link[1] + ":" + self.__getEntityID(link[2]) + "||"
            link_str = link_str[0:-2]
        else:
            link_str = "O"
        return link_str

    def __getCLinkString(self, links, entity_id, source_id):
        link_str = ""
        if source_id in links:
            for link in links[source_id]:
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
        (dct_id, dct_type, dct_value, dct_func) = self.dct
        line += "DCT\t-99\t-99"
        for i in range(8): line += "\tO"    #event  

        #timex    
        line += "\t" + self.__getEntityID(dct_id) + "\tB-" + dct_type + "\t" + dct_value + "\tO\tO\t" + dct_func

        line += "\t" + self.__getLinkString(self.dict_tlinks, dct_id, 0)  #tlinks
        line += "\tO"   #slinks
        line += "\tO"   #alinks
        line += "\tO"   #clinks
        line += "\tO"   #signal
        line += "\tO"   #csignal
        line += "\n\n"

        prev_sent_id = 0
        prev_timex_id = None
        prev_event_id = None
        (tlink_str, slink_str, alink_str, clink_str) = ("O", "O", "O", "O")

        for eid in self.dict_tlinks:
            self.num_tlink += len(self.dict_tlinks[eid])

        #tokens
        for (tok_id, tok_text, tok_sent) in self.arr_tokens:
            if int(tok_sent) != prev_sent_id:
                line += "\n"
                prev_sent_id = int(tok_sent)
                
            line += tok_text + "\t" + tok_id + "\t" + tok_sent

            (tlink_str, slink_str, alink_str, clink_str) = ("O", "O", "O", "O")
            
            #event attributes if any: (event_id, event_class, stem, tense, aspect, polarity, modality, pos)
            if int(tok_id) in self.dict_events:
                (eid, eclass, estem, tense, aspect, pol, mod, pos) = self.dict_events[int(tok_id)]
                line += "\t" + self.__getEntityID(eid)    #event_id

                #event_class, stem
                if prev_event_id != eid:
                    line += "\tB-" + eclass + "\t" + estem
                    prev_event_id = eid
                else: line += "\tI-" + eclass + "\t" + estem

                #tense, aspect, polarity, modality, pos
                line += "\t" + tense + "\t" + aspect + "\t" + pol + "\t" + mod.replace(" ", "_") + "\t" + pos

                tlink_str = self.__getLinkString(self.dict_tlinks, eid, int(tok_id))  #tlinks
                slink_str = self.__getLinkString(self.dict_slinks, eid, int(tok_id))  #slinks
                alink_str = self.__getLinkString(self.dict_alinks, eid, int(tok_id))  #alinks
                clink_str = self.__getCLinkString(self.dict_clinks, eid, int(tok_id)) #clinks

            else:
                prev_event_id = None
                for i in range(8): line += "\tO"

            #timex attributes if any: (timex_id, timex_type, timex_value, anchor, tfunc, tfuncdoc)
            if int(tok_id) in self.dict_timexes:
                (tid, ttype, tvalue, tanchor, tfunc) = self.dict_timexes[int(tok_id)]
                line += "\t" + self.__getEntityID(tid)    #timex_id

                #timex_type
                if prev_timex_id != tid:
                    line += "\tB-" + ttype
                    prev_timex_id = tid
                else: line += "\tI-" + ttype
                
                #timex_value, anchor, tfunc, tfuncdoc
                line += "\t" + tvalue + "\t" + tanchor + "\t" + tfunc + "\tNONE" 

                tlink_str = self.__getLinkString(self.dict_tlinks, tid, int(tok_id))  #tlinks

            else:
                prev_timex_id = None
                for i in range(6): line += "\tO"

            line += "\t" + tlink_str
            line += "\t" + slink_str
            line += "\t" + alink_str
            line += "\t" + clink_str

            #signal attributes if any: signal_id
            if int(tok_id) in self.dict_signals: 
                sid = self.dict_signals[int(tok_id)]
                line += "\t" + self.__getEntityID(sid)
            else: line += "\tO"

            #causal signal attributes if any: csignal_id
            if int(tok_id) in self.dict_csignals: 
                csid = self.dict_csignals[int(tok_id)]
                line += "\t" + self.__getEntityID(csid)
            else: line += "\tO"

            line += "\n"

        return line.strip()
        
    def parseCAT(self):
        cat = ET.parse(self.filename)
        doc = cat.getroot()
        self.docname = doc.get("doc_name")

        mark = doc.find("Markables")
        rel = doc.find("Relations")
        tokens = doc.findall("token")
        events = mark.findall("EVENT")
        timexes = mark.findall("TIMEX3")
        signals = mark.findall("SIGNAL")
        csignals = mark.findall("C-SIGNAL")
        tlinks = rel.findall("TLINK")
        slinks = rel.findall("SLINK")
        alinks = rel.findall("ALINK")
        clinks = rel.findall("CLINK")

        for event in events:
            start_id = event.findall("token_anchor")[0].get("id")
            end_id = event.findall("token_anchor")[-1].get("id")
            (eid, eclass, estem) = (event.get("id"), event.get("class"), event.get("pred"))
            if estem == None: estem = "O"
            (tense, aspect, pol, mod, pos) = (event.get("tense"), event.get("aspect"), event.get("polarity"), event.get("modality"), event.get("pos"))
            if tense == None or tense == "": tense = "O"
            if aspect == None or aspect == "": aspect = "O"
            if pol == None or pol == "": pol = "O"
            if mod == None or mod == "": mod = "O"
            if pos == None or pos == "": pos = "O"
            for i in range(int(start_id), int(end_id)+1):
                self.dict_events[i] = (eid, eclass, estem, tense, aspect, pol, mod, pos)
            self.entities[eid] = ("e", int(start_id), int(end_id))

        for timex in timexes:
            if timex.get("functionInDocument") == "CREATION_TIME": #DCT
                self.dct = (timex.get("id"), timex.get("type"), timex.get("value"), timex.get("functionInDocument"))
                self.entities[timex.get("id")] = ("tmx", 0,0)
            else:            
                start_id = timex.findall("token_anchor")[0].get("id")
                end_id = timex.findall("token_anchor")[-1].get("id")
                (tid, ttype, tvalue, tanchor, tfunc) = (timex.get("id"), timex.get("type"), timex.get("value"), timex.get("anchorTimeID"), timex.get("functionInDocument"))
                if tanchor == None or tanchor == "": tanchor = "O"
                if tfunc == None or tfunc == "": tfunc = "O"
                for i in range(int(start_id), int(end_id)+1):
                    self.dict_timexes[i] = (tid, ttype, tvalue, tanchor, tfunc)
                self.entities[tid] = ("tmx", int(start_id), int(end_id))

        for signal in signals:
            start_id = signal.findall("token_anchor")[0].get("id")
            end_id = signal.findall("token_anchor")[-1].get("id")
            for i in range(int(start_id), int(end_id)+1):
                self.dict_signals[i] = signal.get("id")
            self.entities[signal.get("id")] = ("s", int(start_id), int(end_id))

        for csignal in csignals:
            start_id = csignal.findall("token_anchor")[0].get("id")
            end_id = csignal.findall("token_anchor")[-1].get("id")
            for i in range(int(start_id), int(end_id)+1):
                self.dict_csignals[i] = csignal.get("id")
            self.entities[csignal.get("id")] = ("cs", int(start_id), int(end_id))

        for tlink in tlinks:
            sid = tlink.find("source").get("id")
            tid = tlink.find("target").get("id")
            (_, start_id, end_id) = self.entities[sid]
            for i in range(start_id, end_id+1):
                if i not in self.dict_tlinks:
                    self.dict_tlinks[i] = []
                self.dict_tlinks[i].append((tid, tlink.get("relType"), tlink.get("signalID")))

        for slink in slinks:
            sid = slink.find("source").get("id")
            tid = slink.find("target").get("id")
            (_, start_id, end_id) = self.entities[sid]
            for i in range(start_id, end_id+1):
                if i not in self.dict_slinks:
                    self.dict_slinks[i] = []
                self.dict_slinks[i].append((tid, slink.get("relType"), slink.get("signalID")))

        for alink in alinks:
            sid = alink.find("source").get("id")
            tid = alink.find("target").get("id")
            (_, start_id, end_id) = self.entities[sid]
            for i in range(start_id, end_id+1):
                if i not in self.dict_alinks:
                    self.dict_alinks[i] = []
                self.dict_alinks[i].append((tid, alink.get("relType"), alink.get("signalID")))

        for clink in clinks:
            sid = clink.find("source").get("id")
            tid = clink.find("target").get("id")
            (_, start_id, end_id) = self.entities[sid]
            for i in range(start_id, end_id+1):
                if i not in self.dict_clinks:
                    self.dict_clinks[i] = []
                self.dict_clinks[i].append((tid, clink.get("c-signalID")))

        for t in tokens:
            self.arr_tokens.append((t.get("id"), t.text, t.get("sentence")))

        return self.__buildColumns()

    def getNumTLINK(self):
        return self.num_tlink

#cat_cols = CATToColumns("wsj_1013.xml")
#print cat_cols.parseCAT()
