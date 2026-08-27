;; CPerl mode
(load-library "cperl-mode")

(defalias 'perl-mode 'cperl-mode)
(add-to-list 'auto-mode-alist '("\\.\\([pP][Llm]\\|al\\|psgi\\)\\'" . cperl-mode))
(add-to-list 'interpreter-mode-alist '("perl" . cperl-mode))
(setq cperl-mode-hook
      '(lambda ()
	 (progn
           (defalias 'cperl-db 'perldb)
           )))

;; perldb mode (cperl-db)
(add-hook 'perldb-mode-hook
          (lambda ()
            (local-set-key "\C-a" 'perldb-move-beginning-of-line)
            ))


(custom-set-variables
 '(cperl-auto-newline t)
 '(cperl-auto-newline-after-colon nil)
 '(cperl-autoindent-on-semi t)
 '(cperl-brace-offset 0)
 '(cperl-electric-backspace-untabify t)
 '(cperl-electric-linefeed nil)
 '(cperl-electric-parens nil)
 '(cperl-extra-newline-before-brace nil)
 '(cperl-extra-newline-before-brace-multiline nil)
 '(cperl-font-lock t)
 '(cperl-highlight-variables-indiscriminately nil)
 '(cperl-indent-level 4)
 '(cperl-indet-level 4)
 '(cperl-invalid-face (quote (quote nil)))
 '(cperl-under-as-char t))
(custom-set-faces)

