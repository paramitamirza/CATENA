import re
import os
import subprocess
import xml.etree.ElementTree as ET

import sys;
reload(sys);
sys.setdefaultencoding("utf8")

class CATToTimeML:
    def __init__(self, filename):
        self.filename = filename
        self.docname = ""

        self.arr_tokens = []
        self.event_starts = {}
        self.arr_event_ends = []
        self.event_instances = {}

        self.timex_starts = {}
        self.arr_timex_ends = []

        self.signal_starts = {}
        self.arr_signal_ends = []
        self.csignal_starts = {}
        self.arr_csignal_ends = []

        self.dct = None

        self.arr_tlinks = []
        self.arr_slinks = []
        self.arr_alinks = []
        self.arr_clinks = []

    def generateTimeML(self):
        timeml_str = "<?xml version=\"1.0\" ?>\n<TimeML xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"http://timeml.org/timeMLdocs/TimeML_1.2.1.xsd\">\n"
        #DOCID    
        timeml_str += "<DOCID>" + self.docname + "</DOCID>\n"

        #DCT
        timeml_str += "<DCT><TIMEX3 tid=\"t0\" type=\"" + self.dct[1] + "\" value=\"" + self.dct[2] + "\" temporalFunction=\"false\" functionInDocument=\"" + self.dct[3] + "\">" + self.dct[2] + "</TIMEX3></DCT>\n"

        #TEXT
        text = ""
        prev_sid = 0
        for (tid, tok, sid) in self.arr_tokens:
            if int(sid) != prev_sid: 
                text += "\n"
                prev_sid = int(sid)

            if tid in self.event_starts:
                text += " <EVENT eid=\"e" + self.event_starts[tid][0] + "\" class=\"" + self.event_starts[tid][1] + "\""
                if self.event_starts[tid][2] != "" and self.event_starts[tid][2] != None: text += " stem=\""+ self.event_starts[tid][2] + "\""
                text += ">" + tok
                if tid in self.arr_event_ends:
                    text += "</EVENT>"
            elif tid in self.arr_event_ends: text += " " + tok + "</EVENT>"
            elif tid in self.timex_starts:
                text += " <TIMEX3 tid=\"t" + self.timex_starts[tid][0] + "\" type=\"" + self.timex_starts[tid][1] + "\" value=\"" + self.timex_starts[tid][2] + "\""
                if self.timex_starts[tid][3] != "" and self.timex_starts[tid][3] != None: text += " anchorTimeID=\"t"+ self.timex_starts[tid][3] + "\""
                if self.timex_starts[tid][4] != "" and self.timex_starts[tid][4] != None: text += " functionInDocument=\"" + self.timex_starts[tid][4] + "\""
                text += ">" + tok
                if tid in self.arr_timex_ends:
                    text += "</TIMEX3>"
            elif tid in self.arr_timex_ends: text += " " + tok + "</TIMEX3>"
            elif tid in self.signal_starts:
                text += " <SIGNAL sid=\"s" + self.signal_starts[tid] + "\">" + tok
                if tid in self.arr_signal_ends:
                    text += "</SIGNAL>"
            elif tid in self.arr_signal_ends: text += " " + tok + "</SIGNAL>"
            elif tid in self.csignal_starts:
                text += " <C-SIGNAL cid=\"c" + self.csignal_starts[tid] + "\">" + tok
                if tid in self.arr_csignal_ends:
                    text += "</C-SIGNAL>"
            elif tid in self.arr_csignal_ends: text += " " + tok + "</C-SIGNAL>"
            else:
                stick = ["'s", "n't", ".", ",", "!", "?", ":", ";"]
                if tok in stick: text += tok
                else: text += " " + tok
            
        text = text.replace("`` ", "\"")
        text = text.replace(" ''", "\"")
        text = text.replace("-LCB- ", "{")
        text = text.replace(" -RCB-", "}")
        text = text.replace("-LRB- ", "(")
        text = text.replace(" -RRB-", ")")
        text = text.replace("-LSB- ", "[")
        text = text.replace(" -RSB-", "]")
        timeml_str += "<TEXT>\n" + text + "\n</TEXT>\n"

        #MAKEINSTANCE
        for eid in self.event_instances:
            timeml_str += "<MAKEINSTANCE eventID=\"e" + eid + "\" eiid=\"ei" + eid + "\" tense=\"" + self.event_instances[eid][0] + "\" aspect=\"" + self.event_instances[eid][1] + "\" polarity=\"" + self.event_instances[eid][2] + "\""
            if self.event_instances[eid][3] != "NONE":
                timeml_str += " modality=\"" + self.event_instances[eid][3] + "\""
            timeml_str += " pos=\"" + self.event_instances[eid][4] + "\"/>\n"

        #TLINK
        for tlink in self.arr_tlinks:
            timeml_str += "<TLINK lid=\"l"+ tlink[0] +"\" relType=\"" + tlink[1] + "\""
            if tlink[2] != "" and tlink[2] != None:
                timeml_str += " signalID=\"" + tlink[2] + "\""
            if tlink[3] in self.event_instances:
                timeml_str += " eventInstanceID=\"ei" + tlink[3] + "\""
            else:
                if tlink[3] == self.dct[0]: timeml_str += " timeID=\"t0\"" 
                else: timeml_str += " timeID=\"t" + tlink[3] + "\""
            if tlink[4] in self.event_instances:
                timeml_str += " relatedToEventInstance=\"ei" + tlink[4] + "\""
            else:
                if tlink[4] == self.dct[0]: timeml_str += " relatedToTime=\"t0\"" 
                else: timeml_str += " relatedToTime=\"t" + tlink[4] + "\""
            timeml_str += "/>\n"

        #SLINK
        for slink in self.arr_slinks:
            timeml_str += "<SLINK lid=\"l"+ slink[0] +"\" relType=\"" + slink[1] + "\""
            if slink[2] != "" and slink[2] != None:
                timeml_str += " signalID=\"" + slink[2] + "\""
            timeml_str += " eventInstanceID=\"ei" + slink[3] + "\""
            timeml_str += " subordinatedEventInstance=\"ei" + slink[4] + "\""
            timeml_str += "/>\n"

        #ALINK
        for alink in self.arr_alinks:
            timeml_str += "<ALINK lid=\"l"+ alink[0] +"\" relType=\"" + alink[1] + "\""
            if alink[2] != "" and alink[2] != None:
                timeml_str += " signalID=\"" + alink[2] + "\""
            timeml_str += " eventInstanceID=\"ei" + alink[3] + "\""
            timeml_str += " relatedToEventInstance=\"ei" + alink[4] + "\""
            timeml_str += "/>\n"

        #CLINK
        for clink in self.arr_clinks:
            timeml_str += "<CLINK lid=\"l"+ clink[0] +"\""
            if clink[1] != "" and clink[1] != None:
                timeml_str += " c-signalID=\"c" + clink[1] + "\""
            timeml_str += " eventInstanceID=\"ei" + clink[2] + "\""
            timeml_str += " relatedToEventInstance=\"ei" + clink[3] + "\""
            timeml_str += "/>\n"

        timeml_str += "</TimeML>\n"
        return timeml_str

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
            self.event_starts[start_id] = (event.get("id"), event.get("class"), event.get("pred"))
            self.arr_event_ends.append(end_id)
            if event.get("tense") != None and event.get("aspect") != None and event.get("polarity") != None and event.get("modality") != None and event.get("pos") != None:
                self.event_instances[event.get("id")] = (event.get("tense"), event.get("aspect"), event.get("polarity"), event.get("modality"), event.get("pos"))

        for timex in timexes:
            if timex.get("TAG_DESCRIPTOR") == "Empty_Mark": #DCT
                self.dct = (timex.get("id"), timex.get("type"), timex.get("value"), timex.get("functionInDocument"))
            else:            
                start_id = timex.findall("token_anchor")[0].get("id")
                end_id = timex.findall("token_anchor")[-1].get("id")
                self.timex_starts[start_id] = (timex.get("id"), timex.get("type"), timex.get("value"), timex.get("anchorTimeID"), timex.get("functionInDocument"))
                self.arr_timex_ends.append(end_id)

        for signal in signals:
            start_id = signal.findall("token_anchor")[0].get("id")
            end_id = signal.findall("token_anchor")[-1].get("id")
            self.signal_starts[start_id] = signal.get("id")
            self.arr_signal_ends.append(end_id)

        for csignal in csignals:
            start_id = csignal.findall("token_anchor")[0].get("id")
            end_id = csignal.findall("token_anchor")[-1].get("id")
            self.csignal_starts[start_id] = csignal.get("id")
            self.arr_csignal_ends.append(end_id)

        for tlink in tlinks:
            sid = tlink.find("source").get("id")
            tid = tlink.find("target").get("id")
            self.arr_tlinks.append((tlink.get("id"), tlink.get("relType"), tlink.get("signalID"), sid, tid))

        for slink in slinks:
            sid = slink.find("source").get("id")
            tid = slink.find("target").get("id")
            self.arr_slinks.append((slink.get("id"), slink.get("relType"), slink.get("signalID"), sid, tid))

        for alink in alinks:
            sid = alink.find("source").get("id")
            tid = alink.find("target").get("id")
            self.arr_alinks.append((alink.get("id"), alink.get("relType"), alink.get("signalID"), sid, tid))

        for clink in clinks:
            sid = clink.find("source").get("id")
            tid = clink.find("target").get("id")
            self.arr_clinks.append((clink.get("id"), clink.get("c-signalID"), sid, tid))

        for t in tokens:
            self.arr_tokens.append((t.get("id"), t.text, t.get("sentence")))

        return self.generateTimeML()

        

