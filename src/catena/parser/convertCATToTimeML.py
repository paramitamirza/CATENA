import sys
import os
from converter import CATToTimeML

import sys;
reload(sys);
sys.setdefaultencoding("utf8")

#check directory existence
def ensureDir(f):
    d = os.path.dirname(f)
    if not os.path.exists(d):
        os.makedirs(d)
    
def printUsage():
    print "usage: python convertCATToTimeML.py dir_name [options]"
    print "   or: python convertCATToTimeML.py file_name [options]"
    print " "
    print "       options: -o output_dir_name/file_name (default: dir_path/dir_name_TimeML/ for directory and file_path/file_name.tml for file)"

#main
if __name__ == '__main__':
    
    if len(sys.argv) < 2:
        printUsage()
    else:
        output = ""       
        if len(sys.argv) > 2:
            for i in range(2, len(sys.argv)):
                if sys.argv[i] == "-o":
                    if i+1 < len(sys.argv): output = sys.argv[i+1]

        if os.path.isdir(sys.argv[1]):  #input is directory name
            dirpath = sys.argv[1]
            if dirpath[-1] != "/": dirpath += "/"

            if output != "":
                output_dir_name = output
            else:
                output_dir_name = os.path.dirname(dirpath) + "_TimeML/"

            if output_dir_name[-1] != "/": output_dir_name += "/"
            ensureDir(output_dir_name)

            for r, d, f in os.walk(dirpath):
                for filename in f:
                    #print filename
                    if filename.endswith(".xml"):
                        filepath = os.path.join(r, filename)
                        print "Converting " + filepath + "..."
                        out_file = open(output_dir_name + os.path.basename(filepath).replace(".xml", ".tml"), "w")
                        cat_timeml = CATToTimeML.CATToTimeML(filepath)
                        out_file.write(cat_timeml.parseCAT())
                        out_file.close()

            print "TimeML file(s) are saved in " + output_dir_name

        elif os.path.isfile(sys.argv[1]):   #input is file name
            print "Converting " + sys.argv[1] + "..."

            if output != "":
                out_file_name = output
            else:
                out_file_name = os.path.splitext(os.path.basename(sys.argv[1]))[0] + ".tml"
            out_file = open(out_file_name, "w")

            cat_timeml = CATToTimeML.CATToTimeML(sys.argv[1])
            out_file.write(cat_timeml.parseCAT())
            out_file.close()

            print "TimeML file is saved in " + out_file_name

        else:
            print "File/directory " + sys.argv[1] + " doesn't exist."
                    
