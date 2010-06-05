minFreq = 5

escapeXml :: String -> String
escapeXml s = concat $ map escapeXml' s
  where
    escapeXml' '"' = "&quot;"
    escapeXml' '\'' = "&apos;"
    escapeXml' '&' = "&amp;"
    escapeXml' '<' = "&lt;"
    escapeXml' '>' = "&gt;"
    escapeXml' c = [c]

-- get word and frequency from line
parseLine :: String -> (String, Int)
parseLine line = (word, freq)
  where
    spaceDelimited = words line
    freq = read (last spaceDelimited) :: Int
    word = escapeXml rawWord
    rawWord = unwords $ take (length spaceDelimited - 1) spaceDelimited

showLine :: (String, Int) -> String
showLine (word, freq) = "<w f=\"" ++ (show freq) ++ "\">" ++ word ++ "</w>\n"

showFile :: String -> String
showFile s =
  "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
  ++ "<wordlist>\n"
  ++ s
  ++ "</wordlist>\n"

main = interact (showFile
                 . concat
                 . (map showLine)
                 . (filter (\(_, freq) -> freq > minFreq))
                 . (map parseLine)
                 . lines)
