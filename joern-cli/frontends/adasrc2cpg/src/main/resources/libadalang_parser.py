#!/usr/bin/env python3
"""
Libadalang parser script for adasrc2cpg.
Parses Ada source files and outputs AST as JSON.
"""
import sys
import json

try:
    import libadalang as lal
except ImportError:
    print("Error: libadalang is not installed. Install with: pip install libadalang", file=sys.stderr)
    sys.exit(1)


def node_to_dict(node):
    """Convert a libadalang node to a dictionary"""
    if node is None:
        return None
    
    result = {
        "kind": node.kind_name,
        "text": node.text if hasattr(node, 'text') else "",
        "sloc_range": {
            "start": {"line": node.sloc_range.start.line, "column": node.sloc_range.start.column},
            "end": {"line": node.sloc_range.end.line, "column": node.sloc_range.end.column}
        } if hasattr(node, 'sloc_range') and node.sloc_range else None,
        "children": []
    }
    
    # Add children
    for child in node:
        if child is not None:
            result["children"].append(node_to_dict(child))
    
    return result


def parse_ada_file(input_file, output_file):
    """Parse an Ada file and write the AST as JSON"""
    try:
        # Create analysis context
        context = lal.AnalysisContext()
        
        # Parse the file
        unit = context.get_from_file(input_file)
        
        if unit.root is None:
            print(f"Error: Failed to parse {input_file}", file=sys.stderr)
            if unit.diagnostics:
                for diag in unit.diagnostics:
                    print(f"  {diag}", file=sys.stderr)
            return False
        
        # Convert AST to dictionary
        ast_dict = {
            "file": input_file,
            "root": node_to_dict(unit.root)
        }
        
        # Write to JSON file
        with open(output_file, 'w') as f:
            json.dump(ast_dict, f, indent=2)
        
        return True
        
    except Exception as e:
        print(f"Error parsing {input_file}: {e}", file=sys.stderr)
        return False


if __name__ == "__main__":
    if len(sys.argv) != 3:
        print("Usage: python3 libadalang_parser.py <input_ada_file> <output_json_file>", file=sys.stderr)
        sys.exit(1)
    
    input_file = sys.argv[1]
    output_file = sys.argv[2]
    
    success = parse_ada_file(input_file, output_file)
    sys.exit(0 if success else 1)
