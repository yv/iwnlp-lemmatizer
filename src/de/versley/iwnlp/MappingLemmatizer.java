package de.versley.iwnlp;

/*
Copyright 2017 Yannick Versley

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

import org.jibx.runtime.BindingDirectory;
import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.IUnmarshallingContext;
import org.jibx.runtime.JiBXException;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The class MappingLemmatizer is a simple lemmatizer based on a fixed mapping from surface forms
 * to possible lemmas, which is only filtered by POS information and nothing else.
 */
public class MappingLemmatizer {
    Map<String, Root.WordForm> _mapping;
    private static IBindingFactory bfact;

    static {
        try {
            bfact = BindingDirectory.getFactory(Root.class);
        } catch (JiBXException ex) {
            throw new RuntimeException(ex);
        }
    }

    public MappingLemmatizer(Root mappingOrig) {
        Map<String, String> tagMap = new HashMap<>();
        _mapping = new HashMap<String, Root.WordForm>();
        for (Root.WordForm form: mappingOrig.getWordFormList()) {
            _mapping.put(form.getForm(), form);
            // convert tags to uppercase
            for (Root.WordForm.LemmatizerItem item: form.getLemmaList()) {
                String unnormTag = item.getPOS();
                if (tagMap.containsKey(unnormTag)) {
                    item.setPOS(tagMap.get(tagMap.get(unnormTag)));
                } else {
                    String normTag = unnormTag.toUpperCase();
                    tagMap.put(unnormTag, normTag);
                    item.setPOS(normTag);
                }
                // use the main form string in all entries
                if (item.getForm().equals(form.getForm())) {
                    item.setForm(form.getForm());
                }
            }
        }
    }

    public String lemmatizeSingle(String form, String tag, boolean addMarker) {
        Root.WordForm wf = _mapping.get(form.toLowerCase());
        if (wf == null) {
            return addMarker?"*"+form:form;
        } else {
            int match_score = -1;
            String found = null;
            for (Root.WordForm.LemmatizerItem item : wf.getLemmaList()) {
                if (tag != null && tag.equals(item.getPOS())) {
                    found = item.getLemma();
                    match_score = 1;
                } else if (match_score < 1) {
                    found = addMarker?"?"+item.getLemma():item.getLemma();
                    match_score = 0;
                }
            }
            return found;
        }
    }

    public List<String> lemmatize(List<String> forms, List<String> tags) {
        List<String> result = new ArrayList<>();
        for (int i=0; i<forms.size(); i++) {
            String form = forms.get(i);
            String tag = tags.get(i);
                result.add(lemmatizeSingle(form, tag, true));
        }
        return result;
    }

    public static MappingLemmatizer load(InputStream inputStream) {
        try {
            IUnmarshallingContext uctx = bfact.createUnmarshallingContext();
            Root doc = (Root) uctx.unmarshalDocument(inputStream, null);
            return new MappingLemmatizer(doc);
        } catch (JiBXException ex) {
            throw new RuntimeException("Cannot load lemma table", ex);
        }
    }

    public static void main(String[] args) {
        try {
            MappingLemmatizer lemmatizer = load(
                    new FileInputStream(args[0]));
            String line;
            BufferedReader rd = new BufferedReader(new InputStreamReader(System.in, Charset.forName("UTF-8")));
            while ((line = rd.readLine()) != null) {
                String[] fields = line.split("\\s+");
                if (fields.length >= 2) {
                    String lemma = lemmatizer.lemmatizeSingle(fields[0], fields[1], true);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
